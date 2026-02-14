import os
import random
import datetime
import uuid
import json
import time
import socket
import urllib.request
import urllib.error
import urllib.parse
import re
from decimal import Decimal, getcontext
import faker
import numpy as np
from pathlib import Path

# 尝试导入 pymysql 和 elasticsearch
# 这是一个"一键式"脚本，用于在本地开发环境中快速填充 MySQL, Doris, Elasticsearch 数据
try:
    import pymysql
    MYSQL_AVAILABLE = True
except ImportError:
    MYSQL_AVAILABLE = False
    print("警告: 未找到 'pymysql' 模块，将无法直接同步到 MySQL/Doris。请运行 'pip install pymysql'")

try:
    from elasticsearch import Elasticsearch, helpers
    ES_AVAILABLE = True
except ImportError:
    ES_AVAILABLE = False
    print("警告: 未找到 'elasticsearch' 模块，将无法同步到 ES。请运行 'pip install elasticsearch'")

# 初始化 Faker
fake = faker.Faker(['zh_CN', 'en_US', 'ja_JP'])

# 文件路径配置
MYSQL_FILE = "mysql_chaos_data.sql"
DORIS_FILE = "doris_chaos_data.sql"

def _env_int(name, default):
    v = os.getenv(name)
    if v is None or str(v).strip() == "":
        return int(default)
    return int(v)

def _env_float(name, default):
    v = os.getenv(name)
    if v is None or str(v).strip() == "":
        return float(default)
    return float(v)

def _env_bool(name, default):
    v = os.getenv(name)
    if v is None or str(v).strip() == "":
        return bool(default)
    s = str(v).strip().lower()
    return s in ("1", "true", "yes", "y", "on")

def _clamp_str(s, max_len):
    if s is None:
        return None
    s = str(s)
    if len(s) <= max_len:
        return s
    return s[:max_len]

def _can_connect(host, port, timeout_sec=5.0):
    try:
        with socket.create_connection((host, int(port)), timeout=timeout_sec):
            return True
    except Exception:
        return False

def _parse_url_host_port(url, default_port):
    try:
        u = urllib.parse.urlparse(url)
        host = u.hostname or "localhost"
        port = int(u.port or default_port)
        return host, port
    except Exception:
        return "localhost", int(default_port)

# 数据库连接配置 (根据 docker-compose.yml)
MYSQL_CONFIG = {
    'host': os.getenv("CW_MYSQL_HOST", "localhost"),
    'port': _env_int("CW_MYSQL_PORT", 3306),
    'user': os.getenv("CW_MYSQL_USER", "root"),
    'password': os.getenv("CW_MYSQL_PASSWORD", "root"),
    'db': os.getenv("CW_MYSQL_DB", "culinary_user"),
    'charset': os.getenv("CW_MYSQL_CHARSET", "utf8mb4")
}

DORIS_CONFIG = {
    'host': os.getenv("CW_DORIS_HOST", "localhost"),
    'port': _env_int("CW_DORIS_PORT", 9030), # FE Query Port
    'user': os.getenv("CW_DORIS_USER", "root"),
    'password': os.getenv("CW_DORIS_PASSWORD", ""),
    'db': os.getenv("CW_DORIS_DB", "culinary_dw"),
    'charset': os.getenv("CW_DORIS_CHARSET", "utf8mb4")
}

ES_HOST = os.getenv("CW_ES_HOST", "http://localhost:9200").rstrip("/")

# 生成配置
SYSTEM_YEARS = _env_int("CW_SYSTEM_YEARS", 4)
NOW_DT = datetime.datetime.now()
SYSTEM_START_DATE = (NOW_DT - datetime.timedelta(days=365 * SYSTEM_YEARS)).date()
SYSTEM_END_DATE = NOW_DT.date()

RANDOM_SEED = os.getenv("CW_RANDOM_SEED")
if RANDOM_SEED is not None and str(RANDOM_SEED).strip() != "":
    try:
        seed_val = int(str(RANDOM_SEED).strip())
    except Exception:
        seed_val = int(uuid.uuid5(uuid.NAMESPACE_DNS, str(RANDOM_SEED)).int % (2**31 - 1))
    random.seed(seed_val)
    np.random.seed(seed_val % (2**32 - 1))

APPEND_MODE = _env_bool("CW_APPEND_MODE", True)
RESET_DB = _env_bool("CW_RESET_DB", False)
RESET_ES_INDEX = _env_bool("CW_RESET_ES_INDEX", False)

USER_COUNT = _env_int("CW_USER_COUNT", 5000)
RECIPE_COUNT = _env_int("CW_RECIPE_COUNT", 2500)
TRANS_COUNT = _env_int("CW_TRANS_COUNT", 20000)
ORDER_COUNT = _env_int("CW_ORDER_COUNT", 8000)
EXTRA_PRODUCT_COUNT = _env_int("CW_EXTRA_PRODUCT_COUNT", 120)
FOLLOW_EDGE_TARGET = _env_int("CW_FOLLOW_EDGE_TARGET", USER_COUNT * 12)
MAX_RECIPE_INTERACTIONS_PER_RECIPE = _env_int("CW_MAX_RECIPE_INTERACTIONS_PER_RECIPE", 8000)
MAX_RECIPE_COMMENTS_PER_RECIPE = _env_int("CW_MAX_RECIPE_COMMENTS_PER_RECIPE", 600)
SIGNIN_LOOKBACK_DAYS = _env_int("CW_SIGNIN_LOOKBACK_DAYS", 540)

# 混沌工程配置 (Chaos Engineering)
CHAOS_PROBABILITY = _env_float("CW_CHAOS_PROBABILITY", 0.15)
SPECIAL_CHARS = [
    "Robert'); DROP TABLE Students;--", 
    "🌟✨🔥", 
    "   ", 
    "\\u0000", 
    "admin", 
    "<script>alert('XSS')</script>", 
    "null", 
    "undefined", 
    "1 OR 1=1", 
    "Thinking... \n Multi-line", 
    "😊😂🤣❤️😍", 
    "very_long_string_" * 10 
]

def get_chaos_string(base_str):
    """
    根据设定的概率，将正常字符串替换为脏数据或特殊字符。
    用于测试系统的健壮性（如 SQL 注入防御、XSS 防御、特殊字符显示等）。
    """
    if random.random() < CHAOS_PROBABILITY:
        return random.choice(SPECIAL_CHARS)
    return base_str

def get_pareto_amount():
    """
    使用帕累托分布生成交易金额，模拟真实的消费场景（二八定律）。
    """
    amount = (np.random.pareto(a=1.16) + 1) * 10
    return round(Decimal(amount), 2)

def get_random_time_distribution(base_date):
    """
    生成符合人类活动规律的时间分布 (双峰分布：午餐和晚餐时间高峰)
    """
    hour = 0
    rand = random.random()
    if rand < 0.1: # 深夜 0-6点
        hour = random.randint(0, 6)
    elif rand < 0.3: # 早餐/上午 7-10点
        hour = random.randint(7, 10)
    elif rand < 0.6: # 午餐高峰 11-13点
        hour = random.randint(11, 13)
    elif rand < 0.7: # 下午茶 14-17点
        hour = random.randint(14, 17)
    elif rand < 0.95: # 晚餐高峰 18-21点
        hour = random.randint(18, 21)
    else: # 夜宵 22-23点
        hour = random.randint(22, 23)
        
    minute = random.randint(0, 59)
    second = random.randint(0, 59)
    return datetime.datetime.combine(base_date, datetime.time(hour, minute, second))

def _dt_to_mysql(dt):
    return dt.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]

def _rand_dt_between(start_dt, end_dt):
    if end_dt <= start_dt:
        return start_dt
    delta_ms = int((end_dt - start_dt).total_seconds() * 1000)
    offset_ms = random.randint(0, max(delta_ms, 0))
    return start_dt + datetime.timedelta(milliseconds=offset_ms)

def _mysql_max_id(conn, table, id_col="id"):
    with conn.cursor() as cursor:
        cursor.execute(f"SELECT MAX({id_col}) FROM {table}")
        row = cursor.fetchone()
        v = row[0] if row else None
        return int(v) if v is not None else 0

def _mysql_table_exists(conn, table, db_name):
    with conn.cursor() as cursor:
        cursor.execute(
            "SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s LIMIT 1",
            (db_name, table),
        )
        return cursor.fetchone() is not None

def _make_mobile(user_id):
    prefixes = [
        "130", "131", "132", "133", "135", "136", "137", "138", "139",
        "150", "151", "152", "157", "158", "159",
        "170", "171", "172", "173", "175", "176", "177", "178",
        "180", "181", "182", "183", "185", "186", "187", "188", "189",
        "199",
    ]
    prefix = prefixes[int(user_id) % len(prefixes)]
    suffix = str(int(user_id) % 100000000).zfill(8)
    return prefix + suffix

def _safe_chaos_str(base_str, max_len):
    return _clamp_str(get_chaos_string(base_str), max_len)

def _repo_root():
    return Path(__file__).resolve().parents[2]

def _load_mealdb_cover_urls():
    try:
        p = _repo_root() / "sql" / "mealdb_recipes.sql"
        if not p.exists():
            return []
        text = p.read_text(encoding="utf-8", errors="ignore")
        urls = re.findall(r"https://www\.themealdb\.com/images/media/meals/[^'\"\s]+", text)
        uniq = []
        seen = set()
        for u in urls:
            if u in seen:
                continue
            seen.add(u)
            uniq.append(u)
        return uniq
    except Exception:
        return []

_MEALDB_COVER_URLS = _load_mealdb_cover_urls()

def _pick_cover_url(used=None):
    pool = _MEALDB_COVER_URLS
    if not pool:
        return "https://www.themealdb.com/images/media/meals/6ht8141764786479.jpg"
    if used is None:
        return random.choice(pool)
    if len(used) >= len(pool):
        return random.choice(pool)
    for _ in range(12):
        u = random.choice(pool)
        if u not in used:
            return u
    return random.choice(pool)

def _strip_sql_comments(sql_text):
    out = []
    for line in sql_text.splitlines():
        s = line.strip()
        if not s:
            continue
        if s.startswith("--"):
            continue
        out.append(line)
    return "\n".join(out)

def _split_sql_statements(sql_text):
    buf = []
    in_single = False
    in_double = False
    in_backtick = False
    escape = False
    cur = []
    for ch in sql_text:
        if escape:
            cur.append(ch)
            escape = False
            continue
        if ch == "\\":
            cur.append(ch)
            escape = True
            continue
        if ch == "'" and not in_double and not in_backtick:
            in_single = not in_single
            cur.append(ch)
            continue
        if ch == '"' and not in_single and not in_backtick:
            in_double = not in_double
            cur.append(ch)
            continue
        if ch == "`" and not in_single and not in_double:
            in_backtick = not in_backtick
            cur.append(ch)
            continue
        if ch == ";" and not in_single and not in_double and not in_backtick:
            stmt = "".join(cur).strip()
            if stmt:
                buf.append(stmt)
            cur = []
            continue
        cur.append(ch)
    tail = "".join(cur).strip()
    if tail:
        buf.append(tail)
    return buf

def _exec_sql_file(conn, file_path):
    text = Path(file_path).read_text(encoding="utf-8", errors="ignore")
    text = _strip_sql_comments(text)
    stmts = _split_sql_statements(text)
    with conn.cursor() as cursor:
        for stmt in stmts:
            cursor.execute(stmt)
    conn.commit()

def _mysql_connect(include_db=True):
    if not MYSQL_AVAILABLE:
        return None
    conf = dict(MYSQL_CONFIG)
    if not include_db:
        conf.pop("db", None)
    return pymysql.connect(**conf)

def reset_mysql_schema_and_seed():
    if not MYSQL_AVAILABLE:
        print("跳过重置：未检测到 pymysql")
        return
    db_name = MYSQL_CONFIG.get("db") or "culinary_user"
    root = _repo_root()
    ddl = root / "sql" / "mysql.sql"
    seed = root / "sql" / "seed_data.sql"
    fix = root / "sql" / "fix_tables_final.sql"
    if not ddl.exists():
        print(f"跳过重置：未找到 {ddl}")
        return
    conn = _mysql_connect(include_db=False)
    try:
        with conn.cursor() as cursor:
            cursor.execute(f"DROP DATABASE IF EXISTS `{db_name}`")
            cursor.execute(f"CREATE DATABASE IF NOT EXISTS `{db_name}` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci")
        conn.commit()
        with conn.cursor() as cursor:
            cursor.execute(f"USE `{db_name}`")
        conn.commit()
        _exec_sql_file(conn, str(ddl))
        if seed.exists():
            _exec_sql_file(conn, str(seed))
        if fix.exists():
            _exec_sql_file(conn, str(fix))
        print(f"MySQL 重置完成：{db_name}")
    finally:
        try:
            conn.close()
        except Exception:
            pass

_TASTE_TAGS = [
    "清淡", "微甜", "微辣", "中辣", "特辣", "甜口", "咸鲜", "酸甜", "麻辣", "蒜香", "葱香", "姜香", "椒麻", "孜然",
    "番茄味", "咖喱味", "奶香", "巧克力味", "黑胡椒", "芥末", "柠檬香", "烟熏", "香草风味", "焦糖风味", "蜂蜜风味",
    "鲜辣", "酸辣", "香辣", "酱香", "椒盐", "麻酱", "花椒香", "豆豉香", "芝士风味", "抹茶风味", "椰香", "玫瑰香",
]
_DIET_TAGS = [
    "低脂", "高蛋白", "低糖", "低盐", "低卡", "高纤", "素食", "蛋奶素", "无麸质", "不吃辣", "不吃香菜", "不吃葱", "不吃蒜",
    "乳糖不耐受", "海鲜过敏", "坚果过敏", "控碳", "健身餐", "宝宝辅食", "老人友好",
]
_SCENE_TAGS = [
    "早餐", "早午餐", "午餐", "下午茶", "晚餐", "夜宵", "便当", "聚会", "露营", "加班", "宿舍", "一人食", "家庭餐", "带饭",
    "节日", "生日", "周末", "懒人", "减脂期", "增肌期",
]
_EQUIPMENT_TAGS = [
    "空气炸锅", "电饭煲", "烤箱", "微波炉", "平底锅", "砂锅", "蒸锅", "压力锅", "料理机", "破壁机", "电磁炉", "炒锅",
    "面包机", "搅拌机", "打蛋器", "厨师机", "温度计", "电子秤", "刀具", "不粘锅", "铸铁锅",
]
_COOK_METHODS = [
    "清蒸", "红烧", "香煎", "爆炒", "焗烤", "凉拌", "炖煮", "烤制", "油炸", "煮", "煎", "烤", "蒸", "焖", "煨", "煮炖",
    "低温慢煮", "快手", "免烤", "免揉", "免打发", "无油", "少油",
]
_ADJ = [
    "秘制", "升级版", "爆款", "经典", "家庭版", "高颜值", "低脂", "无油", "超快手", "餐厅同款", "零失败", "随手做",
    "一锅出", "高复购", "超下饭", "新手友好", "低预算", "高性价比", "懒人版", "轻食", "轻奢", "硬核", "深夜治愈",
    "低卡", "控糖", "控盐", "高蛋白", "营养均衡",
]

_INGREDIENT_POOLS = {
    "家常菜": [
        "鸡蛋", "番茄", "土豆", "青椒", "茄子", "豆腐", "西兰花", "胡萝卜", "洋葱", "香菇", "金针菇", "杏鲍菇", "木耳",
        "虾仁", "鱿鱼", "带鱼", "三文鱼", "鸡胸肉", "鸡腿", "牛肉", "猪里脊", "五花肉", "培根", "腊肠", "午餐肉",
        "白菜", "生菜", "黄瓜", "玉米", "四季豆", "花菜", "芦笋", "秋葵", "青菜", "菠菜",
        "粉丝", "年糕", "豆芽", "豆皮", "腐竹", "米饭", "面条",
    ],
    "烘焙": [
        "低筋面粉", "高筋面粉", "鸡蛋", "黄油", "无盐黄油", "牛奶", "淡奶油", "酸奶", "细砂糖", "红糖", "蜂蜜", "枫糖浆",
        "可可粉", "抹茶粉", "泡打粉", "小苏打", "酵母", "盐", "香草精", "肉桂粉",
        "芝士", "奶油奶酪", "巧克力", "黑巧克力", "坚果", "葡萄干", "蔓越莓干", "椰蓉",
        "草莓", "蓝莓", "香蕉", "柠檬",
    ],
    "西餐": [
        "牛排", "鸡胸肉", "三文鱼", "虾仁", "培根", "意面", "番茄", "蘑菇", "洋葱", "蒜", "奶油", "芝士", "帕玛森",
        "橄榄油", "黑胡椒", "迷迭香", "百里香", "罗勒", "柠檬", "土豆", "西兰花", "芦笋",
        "生菜", "牛油果", "黄瓜", "彩椒",
    ],
    "饮品": [
        "咖啡", "红茶", "绿茶", "乌龙茶", "抹茶", "可可", "牛奶", "酸奶", "椰奶", "燕麦奶",
        "柠檬", "橙子", "草莓", "蓝莓", "葡萄", "百香果", "芒果", "菠萝", "西瓜", "苹果",
        "蜂蜜", "糖浆", "冰块", "薄荷叶", "苏打水", "气泡水", "炼乳",
    ],
}

_CONDIMENTS = [
    "盐", "白糖", "生抽", "老抽", "蚝油", "料酒", "米醋", "陈醋", "白胡椒", "黑胡椒", "花椒", "辣椒粉", "辣椒面",
    "豆瓣酱", "番茄酱", "沙拉酱", "芝麻酱", "孜然粉", "咖喱块", "香油", "橄榄油", "黄油", "蒜末", "姜丝", "葱花",
]

_IP_CN = {
    "北京": ["北京"],
    "上海": ["上海"],
    "广东": ["广州", "深圳", "佛山", "东莞", "珠海", "中山", "惠州"],
    "浙江": ["杭州", "宁波", "温州", "绍兴", "嘉兴", "金华"],
    "江苏": ["南京", "苏州", "无锡", "常州", "南通", "徐州"],
    "四川": ["成都", "绵阳", "德阳", "乐山", "宜宾"],
    "湖北": ["武汉", "宜昌", "襄阳", "荆州"],
    "山东": ["济南", "青岛", "烟台", "潍坊", "临沂"],
    "福建": ["福州", "厦门", "泉州", "漳州"],
    "重庆": ["重庆"],
    "湖南": ["长沙", "株洲", "湘潭"],
    "河南": ["郑州", "洛阳", "开封"],
    "河北": ["石家庄", "唐山", "保定"],
    "陕西": ["西安", "咸阳", "宝鸡"],
    "辽宁": ["沈阳", "大连", "鞍山"],
    "安徽": ["合肥", "芜湖", "蚌埠"],
    "江西": ["南昌", "赣州", "九江"],
}

_IP_OVERSEA = [
    "海外·美国", "海外·加拿大", "海外·英国", "海外·德国", "海外·法国", "海外·澳大利亚", "海外·新加坡", "海外·日本", "海外·韩国",
    "海外·马来西亚", "海外·泰国",
]

_NET_TYPES = ["WiFi", "5G", "4G", "有线", "未知"]
_APP_CHANNELS = ["AppStore", "Huawei", "Xiaomi", "OPPO", "VIVO", "Tencent", "Web"]
_IOS_MODELS = ["iPhone 11", "iPhone 12", "iPhone 13", "iPhone 14", "iPhone 15", "iPhone 15 Pro", "iPhone SE", "iPad Pro", "iPad Air"]
_ANDROID_MODELS = ["MI 13", "MI 14", "Redmi K60", "HUAWEI Mate 60", "HUAWEI P60", "OPPO Find X7", "vivo X100", "OnePlus 12", "Pixel 8"]
_WEB_BROWSERS = ["Chrome", "Edge", "Safari", "Firefox"]

def _build_ip_location():
    if random.random() < 0.07:
        return random.choice(_IP_OVERSEA)
    prov = random.choice(list(_IP_CN.keys()))
    city = random.choice(_IP_CN.get(prov) or [prov])
    if prov == city:
        return prov
    if random.random() < 0.6:
        return f"{prov}·{city}"
    return prov

def _build_user_region():
    if random.random() < 0.06:
        country = random.choice(["日本", "美国", "新加坡", "加拿大", "英国", "德国", "澳大利亚"])
        if country == "日本":
            return country, "", random.choice(["东京", "大阪", "名古屋", "福冈"])
        if country == "美国":
            return country, "", random.choice(["洛杉矶", "纽约", "旧金山", "西雅图"])
        if country == "新加坡":
            return country, "", "新加坡"
        if country == "加拿大":
            return country, "", random.choice(["多伦多", "温哥华", "蒙特利尔"])
        if country == "英国":
            return country, "", random.choice(["伦敦", "曼彻斯特", "伯明翰"])
        if country == "德国":
            return country, "", random.choice(["柏林", "慕尼黑", "法兰克福"])
        return country, "", random.choice(["悉尼", "墨尔本", "布里斯班"])
    province = random.choice(list(_IP_CN.keys()))
    city = random.choice(_IP_CN.get(province) or [province])
    return "中国", province, city

def _build_device_info():
    platform = random.choices(["iOS", "Android", "Web"], weights=[42, 45, 13])[0]
    app_ver = f"{random.randint(1, 5)}.{random.randint(0, 9)}.{random.randint(0, 30)}"
    net = random.choices(_NET_TYPES, weights=[55, 18, 20, 3, 4])[0]
    if platform == "iOS":
        model = random.choice(_IOS_MODELS)
        os_ver = f"iOS {random.randint(15, 17)}.{random.randint(0, 6)}"
        ch = "AppStore"
        return _clamp_str(f"{platform}/{model} {os_ver}; App {app_ver}; {net}; {ch}", 128)
    if platform == "Android":
        model = random.choice(_ANDROID_MODELS)
        os_ver = f"Android {random.randint(10, 14)}"
        ch = random.choice([c for c in _APP_CHANNELS if c != "AppStore"])
        return _clamp_str(f"{platform}/{model} {os_ver}; App {app_ver}; {net}; {ch}", 128)
    br = random.choice(_WEB_BROWSERS)
    br_ver = f"{random.randint(110, 125)}.{random.randint(0, 9)}"
    return _clamp_str(f"Web/{br} {br_ver}; {net}", 128)

def _build_tip(theme):
    pool = theme.get("pool") or "家常菜"
    main = theme.get("main") or "食材"
    equip = (theme.get("equipment") or [])
    equipment = equip[0] if equip else ""
    method = theme.get("method") or ""
    taste = (theme.get("taste") or [])
    taste_hint = random.choice(taste) if taste else "咸鲜"
    grams = random.randint(120, 380)
    sec = random.choice([20, 30, 45, 60, 90])
    temp = random.choice([160, 170, 180, 190, 200])
    base = [
        f"调味建议：先少后多，最后按 {taste_hint} 方向微调。",
        f"口感要点：{main}别切太小，火候更好掌控。",
        f"收汁小技巧：沿锅边淋少量清水，收至微微挂汁即可。",
        f"时间管理：关键步骤多给 {sec} 秒，成品更稳。",
        f"控油建议：用不粘锅小火慢煎，减少额外用油。",
        f"复刻率提升：调味料提前混匀，按顺序下锅不慌。",
    ]
    if pool == "烘焙":
        base.extend([
            f"烘焙建议：{temp}°C 先观察上色，再按状态加减时间。",
            f"面糊状态：翻拌到无干粉即可，避免出筋影响松软。",
            f"配方尺度：主料约 {grams}g，甜度可按口味调整 10%-20%。",
            "出炉冷却：完全冷却再切更整齐，口感也更稳定。",
        ])
    if pool == "饮品":
        base.extend([
            "甜度策略：先加一半糖浆，试味后再补更不翻车。",
            "风味提升：加一片柠檬或少许薄荷，层次更清爽。",
            "冰量控制：冰多更清爽，冰少风味更浓。",
        ])
    if pool == "西餐":
        base.extend([
            "火候建议：先大火上色，再转中小火定型。",
            "静置要点：出锅静置 2-3 分钟，汁水回流更嫩。",
        ])
    if equipment:
        base.append(f"工具提示：使用 {equipment} 时注意火力，避免糊底或过干。")
    if method:
        base.append(f"做法提示：{method}类菜更看重节奏，提前备料更从容。")
    return _clamp_str(random.choice(base), 180)

_METHOD_POOLS = {
    "家常菜": ["准备食材", "腌制入味", "热锅起油", "爆香调味", "翻炒至熟", "收汁出锅", "装盘点缀"],
    "烘焙": ["预热烤箱", "混合干料", "打发蛋液", "混合面糊", "倒入模具", "烘烤定型", "冷却脱模"],
    "西餐": ["准备配菜", "调味腌制", "加热锅具", "煎制上色", "控制火候", "摆盘", "淋酱完成"],
    "饮品": ["准备杯具", "处理水果", "加入冰块", "混合搅拌", "调整甜度", "装杯", "点缀出品"],
}

_SUBCATEGORY_HINTS = {
    "川菜": ["麻辣", "微辣", "蒜香"],
    "粤菜": ["清淡", "咸鲜"],
    "湘菜": ["中辣", "特辣"],
    "汤羹": ["清淡", "咸鲜"],
    "蛋糕": ["奶香", "甜口", "巧克力味"],
    "面包": ["奶香", "甜口"],
    "饼干": ["甜口", "巧克力味"],
    "牛排": ["黑胡椒", "咸鲜"],
    "意面": ["番茄味", "奶香"],
    "沙拉": ["清淡", "酸甜"],
    "果汁": ["酸甜", "清淡"],
    "茶饮": ["清淡", "微甜"],
    "咖啡": ["奶香", "微甜"],
}

def _pool_key_from_category_name(cat_name):
    name = str(cat_name or "")
    if "烘焙" in name or name in ("蛋糕", "面包", "饼干"):
        return "烘焙"
    if "饮" in name or name in ("果汁", "茶饮", "咖啡"):
        return "饮品"
    if name in ("西餐", "牛排", "意面", "沙拉"):
        return "西餐"
    return "家常菜"

def _pick_unique(items, n):
    if not items:
        return []
    n = max(0, min(int(n), len(items)))
    return random.sample(items, k=n)

def _build_recipe_theme(cat_name):
    key = _pool_key_from_category_name(cat_name)
    ingredient_pool = _INGREDIENT_POOLS.get(key, _INGREDIENT_POOLS["家常菜"])
    method_pool = _METHOD_POOLS.get(key, _METHOD_POOLS["家常菜"])
    main = random.choice(ingredient_pool)
    others = _pick_unique([x for x in ingredient_pool if x != main], random.randint(3, 6))
    base_taste = set(_pick_unique(_TASTE_TAGS, random.randint(1, 2)))
    hint = _SUBCATEGORY_HINTS.get(str(cat_name or ""), [])
    if hint:
        base_taste.update(_pick_unique(hint, 1))
    scene = _pick_unique(_SCENE_TAGS, random.randint(1, 2))
    equip = _pick_unique(_EQUIPMENT_TAGS, random.randint(1, 2))
    method = random.choice(_COOK_METHODS)
    return {
        "pool": key,
        "category": str(cat_name or ""),
        "main": main,
        "ingredients": [main] + others,
        "methods": method_pool,
        "taste": list(base_taste),
        "scene": scene,
        "equipment": equip,
        "method": method,
    }

def _build_recipe_title(theme):
    main = theme.get("main") or "家常菜"
    taste = theme.get("taste") or []
    pool = theme.get("pool") or "家常菜"
    method = theme.get("method") or ""
    style = random.choice(taste) if taste else ""
    if pool == "烘焙":
        suffix = random.choice(["蛋糕", "面包", "饼干", "司康", "玛芬"])
        adj = random.choice(_ADJ)
        return _clamp_str(f"{adj}{style}{main}{suffix}".strip(), 128)
    if pool == "饮品":
        suffix = random.choice(["拿铁", "气泡水", "果茶", "奶昔", "冰饮"])
        adj = random.choice(_ADJ)
        return _clamp_str(f"{adj}{main}{suffix}".strip(), 128)
    if pool == "西餐":
        suffix = random.choice(["意面", "沙拉", "煎烤拼盘", "浓汤"])
        adj = random.choice(_ADJ)
        return _clamp_str(f"{adj}{style}{main}{suffix}".strip(), 128)
    suffix = random.choice(["家常做法", "快手版", "下饭菜", "一锅出", "懒人做法"])
    adj = random.choice(_ADJ)
    return _clamp_str(f"{adj}{method}{style}{main}{suffix}".strip(), 128)

def _build_recipe_desc(theme):
    ingredients = theme.get("ingredients") or []
    taste = theme.get("taste") or []
    scene = theme.get("scene") or []
    equip = theme.get("equipment") or []
    method = theme.get("method") or ""
    seg_a = f"主料：{('、'.join(ingredients[:3]))}" if ingredients else "主料：家常食材"
    seg_b = f"口味：{(' / '.join(taste[:2]))}" if taste else "口味：咸鲜"
    seg_c = f"适合：{('、'.join(scene[:2]))}" if scene else "适合：家庭餐"
    seg_d = f"工具：{('、'.join(equip[:1]))}" if equip else ""
    seg_e = f"做法：{method}" if method else ""
    out = "；".join([x for x in [seg_a, seg_b, seg_c, seg_d, seg_e] if x])
    return out

def _build_recipe_tags(theme):
    tags = []
    for k in ("category", "main"):
        v = theme.get(k)
        if v:
            tags.append(str(v))
    for v in (theme.get("ingredients") or [])[:3]:
        if v:
            tags.append(str(v))
    for k in ("taste", "scene", "equipment"):
        for v in theme.get(k) or []:
            if v:
                tags.append(str(v))
    if theme.get("method"):
        tags.append(str(theme.get("method")))
    for v in _pick_unique(_CONDIMENTS, random.randint(1, 2)):
        tags.append(v)
    for v in _pick_unique(_DIET_TAGS, random.randint(0, 1)):
        tags.append(v)
    for t in _pick_unique(["家常", "下饭", "快手", "低脂", "高蛋白", "养生", "减脂餐", "宝宝辅食", "小白友好", "宵夜", "便当", "聚会"], random.randint(1, 2)):
        tags.append(t)
    uniq = []
    seen = set()
    for t in tags:
        t = str(t).strip()
        if not t or t in seen:
            continue
        seen.add(t)
        uniq.append(t)
    return uniq[:12]

def _build_recipe_steps(theme, n):
    pool = theme.get("pool") or "家常菜"
    main = theme.get("main") or "食材"
    ingredients = theme.get("ingredients") or [main]
    methods = theme.get("methods") or _METHOD_POOLS.get(pool, _METHOD_POOLS["家常菜"])
    taste = theme.get("taste") or []
    equip = theme.get("equipment") or []
    method = theme.get("method") or ""
    condiments = _pick_unique(_CONDIMENTS, random.randint(3, 5))
    c1 = condiments[0] if condiments else "盐"
    c2 = condiments[1] if len(condiments) > 1 else "生抽"
    c3 = condiments[2] if len(condiments) > 2 else "蚝油"
    min_a = random.randint(1, 3)
    min_b = random.randint(2, 8)
    sec = random.choice([20, 30, 45, 60, 90])

    opening = [
        f"准备：将{('、'.join(ingredients[:4]))}备齐，清洗沥干。",
        f"处理：{main}按口感需要切块/切片，其余配菜切好备用。",
    ]
    if pool == "烘焙":
        opening = [
            "预热烤箱至 170°C-180°C，模具铺好油纸或抹油。",
            f"称量：{('、'.join(ingredients[:4]))}按配方称量备用，黄油提前回温。",
        ]
    if pool == "饮品":
        opening = [
            f"准备：将{('、'.join(ingredients[:3]))}洗净处理，杯中加入适量冰块。",
            "打底：根据口味加入蜂蜜/糖浆，搅拌均匀。",
        ]
    if pool == "西餐":
        opening = [
            f"准备：{main}与配菜处理好，厨房纸吸干表面水分。",
            "调味：撒盐与黑胡椒，静置 10 分钟更入味。",
        ]
    if pool != "饮品" and random.random() < 0.65:
        opening.insert(2 if len(opening) > 1 else 1, f"腌制：加入{c1}、{c2}、{c3}拌匀，静置 {min_a}-{min_b} 分钟。")

    middle = []
    for m in methods:
        if m == "热锅起油":
            middle.append(random.choice([
                "热锅后倒入少量油，油温升至微微冒烟。",
                "锅烧热后下油，转中火让油温均匀。",
            ]))
        elif m == "爆香调味":
            middle.append(random.choice([
                f"下蒜末/葱花爆香，加入{c2}与少许{c1}提味。",
                f"爆香后加入{c2}、{c3}，快速翻炒让香味释放。",
            ]))
        elif m == "翻炒至熟":
            middle.append(random.choice([
                f"下入{main}与配菜翻炒至断生，保持大火快炒更香。",
                f"{main}先下锅定型，再下配菜同炒，避免出水过多。",
            ]))
        elif m == "收汁出锅":
            middle.append(random.choice([
                "沿锅边淋少量清水，收至略挂汁即可出锅。",
                f"收汁到锅底只剩薄薄一层，最后再给 {sec} 秒大火收香。",
            ]))
        elif m == "预热烤箱":
            middle.append("再次确认烤箱预热到位。")
        elif m == "混合干料":
            middle.append("将面粉、泡打粉等干料混匀并过筛。")
        elif m == "打发蛋液":
            middle.append("鸡蛋加糖打发至体积变大、颜色变浅。")
        elif m == "混合面糊":
            middle.append("干料分次加入，翻拌至无干粉即可，避免过度搅拌。")
        elif m == "倒入模具":
            middle.append("将面糊倒入模具，轻震去大气泡。")
        elif m == "烘烤定型":
            middle.append("送入烤箱中层，烘烤至表面上色、牙签插入无湿糊。")
        elif m == "冷却脱模":
            middle.append("出炉冷却后脱模，完全冷却再切更整齐。")
        elif m == "处理水果":
            middle.append("水果切块/挤汁，避免带籽影响口感。")
        elif m == "混合搅拌":
            middle.append("加入液体与水果，用搅拌机搅打 30-60 秒。")
        elif m == "调整甜度":
            middle.append("试味后补充蜂蜜/糖浆/柠檬汁，平衡酸甜。")
        elif m == "装杯":
            middle.append("倒入杯中，冰量可按喜好调整。")
        elif m == "点缀出品":
            garnish = random.choice(["薄荷叶", "柠檬片", "可可粉", "肉桂粉", "坚果碎", "芝士碎"])
            middle.append(f"点缀：撒上{garnish}，口感更丰富。")
        elif m == "加热锅具":
            middle.append("平底锅烧热，加入少量橄榄油。")
        elif m == "煎制上色":
            middle.append(f"{main}两面煎至上色，锁住肉汁。")
        elif m == "控制火候":
            middle.append("转中小火继续加热至熟度合适。")
        elif m == "摆盘":
            middle.append("搭配配菜摆盘，淋少许橄榄油即可。")
        elif m == "淋酱完成":
            middle.append("淋上酱汁，撒少许黑胡椒完成。")
        else:
            middle.append(f"{m}，按口感调整火候与时间。")
    if pool != "饮品" and random.random() < 0.35:
        middle.insert(max(1, len(middle) - 1), random.choice([
            f"中途补少量热水，盖盖焖 {random.randint(1, 4)} 分钟更入味。",
            f"尝味后按 {random.choice(taste) if taste else '咸鲜'} 方向微调，少量多次更稳。",
        ]))

    closing = []
    if taste:
        closing.append(f"最后尝味：根据口味补少许{random.choice(taste)}方向的调味。")
    if equip:
        closing.append(f"小贴士：使用{equip[0]}时注意火力，避免糊底。")
    if method and random.random() < 0.5:
        closing.append(f"做法提示：{method}更看重节奏，提前备料更从容。")
    if not closing:
        closing.append("完成：装盘即可开吃，趁热口感更好。")

    steps = opening + middle + closing
    if len(steps) >= n:
        return steps[:n]
    while len(steps) < n:
        steps.insert(max(1, len(steps) - 1), random.choice([
            "中途可适当翻动，受热更均匀。",
            "如果偏干可补少量热水或牛奶，避免糊底。",
            "喜欢更香可加少许黄油/橄榄油，立刻提香。",
            "口味重可加少许胡椒或蒜末，别一次下太多。",
            f"出锅前撒少许{random.choice(['葱花', '白芝麻', '黑胡椒', '辣椒面'])}，香气更立体。",
            f"调味料先混匀再下锅，复刻更稳定。",
        ]))
    return steps[:n]

def _build_user_interests(favorite_cuisine, taste_preference, dietary_restrictions):
    tags = []
    if favorite_cuisine:
        tags.append(str(favorite_cuisine))
    if taste_preference:
        tags.append(str(taste_preference))
    if dietary_restrictions and dietary_restrictions != "无":
        tags.append(str(dietary_restrictions))
    tags.extend(_pick_unique(_SCENE_TAGS, random.randint(1, 2)))
    tags.extend(_pick_unique(_EQUIPMENT_TAGS, random.randint(1, 2)))
    tags.extend(_pick_unique(["烘焙", "咖啡", "下厨打卡", "菜谱收藏", "健康饮食", "减脂餐", "家庭餐", "周末做饭", "懒人快手", "一锅出", "露营料理", "学生党", "宝妈辅食", "低预算", "高性价比"], random.randint(2, 4)))
    uniq = []
    seen = set()
    for t in tags:
        t = str(t).strip()
        if not t or t in seen:
            continue
        seen.add(t)
        uniq.append(t)
    return ",".join(uniq[:10])

def _build_comment_text(theme, step_count, as_reply=False):
    main = theme.get("main") or "这道菜"
    ingredients = theme.get("ingredients") or [main]
    taste = theme.get("taste") or []
    scene = theme.get("scene") or []
    equip = theme.get("equipment") or []
    i1 = ingredients[0] if ingredients else main
    i2 = ingredients[1] if len(ingredients) > 1 else i1
    stp = random.randint(1, max(1, int(step_count or 1)))
    tone = random.choice(["新人", "复刻", "家常", "加班", "懒人", "健身"])
    if as_reply:
        return random.choice([
            f"我也按第{stp}步做了，火候别太大就很稳。",
            f"{i1}我换成了{i2}，风味也不错。",
            f"同感，{(' / '.join(taste[:1]) or '咸鲜')}方向真的很适合。",
            f"用{equip[0]}做会更省事。" if equip else f"我喜欢最后收汁那一步。",
            f"第{stp}步我多收了 20 秒，{i1}更入味。",
            f"{('、'.join(scene[:1]) or '日常')}做这道真的省心，收藏了。",
        ])
    return random.choice([
        f"复刻成功：{i1}很香，按第{stp}步的火候做就不翻车。",
        f"{tone}友好的一道：食材常见，{('、'.join(scene[:1]) or '日常')}做很合适。",
        f"建议：{i1}别切太小，口感会更好。",
        f"{i1}我加了点{i2}，味道更有层次。",
        f"{(' / '.join(taste[:2]) or '咸鲜')}的味道很舒服，家里人都说好吃。",
        f"想要更香：最后加一点点蒜末/胡椒，立刻提升。",
        f"成本不高但很能打，{('、'.join(scene[:1]) or '一人食')}首选。",
    ])

class DataFactory:
    """
    数据工厂类
    负责生成一致性的模拟数据，并同步到 MySQL, Doris, Elasticsearch。
    """
    def __init__(self):
        self.users = []
        self.user_profiles = []
        self.user_stats = []
        self.transactions = []
        self.recipes = []
        self.recipe_steps = []
        self.recipe_tag_relations = []
        self.categories = []
        self.stats = []
        self.comments = []
        self.interactions = []
        self.follows = []
        self.points_records = []
        self.products = []
        self.orders = []
        self.order_items = []
        self.notifications = []
        self.daily_stats = {} # 用于 Doris 聚合校验
        self._id_base = {
            "user": 0,
            "recipe": 10000,
            "step": 0,
            "interaction": 0,
            "transaction": 0,
            "comment": 0,
            "product": 0,
            "order": 0,
            "order_item": 0,
        }

    def set_id_base(self, **kwargs):
        for k, v in kwargs.items():
            if v is None:
                continue
            self._id_base[k] = int(v)

    def generate_categories(self):
        """生成食谱分类数据"""
        print("正在生成分类数据 (Categories)...")
        base = [
            {"id": 1, "name": "家常菜", "parent_id": 0, "level": 1, "sort": 1},
            {"id": 2, "name": "烘焙", "parent_id": 0, "level": 1, "sort": 2},
            {"id": 3, "name": "西餐", "parent_id": 0, "level": 1, "sort": 3},
            {"id": 4, "name": "饮品", "parent_id": 0, "level": 1, "sort": 4},
            {"id": 5, "name": "川菜", "parent_id": 1, "level": 2, "sort": 5},
            {"id": 6, "name": "粤菜", "parent_id": 1, "level": 2, "sort": 6},
            {"id": 7, "name": "湘菜", "parent_id": 1, "level": 2, "sort": 7},
            {"id": 8, "name": "汤羹", "parent_id": 1, "level": 2, "sort": 8},
            {"id": 9, "name": "蛋糕", "parent_id": 2, "level": 2, "sort": 9},
            {"id": 10, "name": "面包", "parent_id": 2, "level": 2, "sort": 10},
            {"id": 11, "name": "饼干", "parent_id": 2, "level": 2, "sort": 11},
            {"id": 12, "name": "牛排", "parent_id": 3, "level": 2, "sort": 12},
            {"id": 13, "name": "意面", "parent_id": 3, "level": 2, "sort": 13},
            {"id": 14, "name": "沙拉", "parent_id": 3, "level": 2, "sort": 14},
            {"id": 15, "name": "果汁", "parent_id": 4, "level": 2, "sort": 15},
            {"id": 16, "name": "茶饮", "parent_id": 4, "level": 2, "sort": 16},
            {"id": 17, "name": "咖啡", "parent_id": 4, "level": 2, "sort": 17},
        ]
        extra = [
            {"id": 18, "name": "轻食", "parent_id": 0, "level": 1, "sort": 18},
            {"id": 19, "name": "健身餐", "parent_id": 18, "level": 2, "sort": 19},
            {"id": 20, "name": "低脂沙拉", "parent_id": 18, "level": 2, "sort": 20},
            {"id": 21, "name": "日料", "parent_id": 0, "level": 1, "sort": 21},
            {"id": 22, "name": "寿司", "parent_id": 21, "level": 2, "sort": 22},
            {"id": 23, "name": "拉面", "parent_id": 21, "level": 2, "sort": 23},
            {"id": 24, "name": "韩餐", "parent_id": 0, "level": 1, "sort": 24},
            {"id": 25, "name": "拌饭", "parent_id": 24, "level": 2, "sort": 25},
            {"id": 26, "name": "锅物", "parent_id": 24, "level": 2, "sort": 26},
            {"id": 27, "name": "小吃", "parent_id": 0, "level": 1, "sort": 27},
            {"id": 28, "name": "早餐饼", "parent_id": 27, "level": 2, "sort": 28},
            {"id": 29, "name": "炸物", "parent_id": 27, "level": 2, "sort": 29},
        ]
        self.categories.extend(base)
        if random.random() < 0.85:
            self.categories.extend(extra)

    def generate_users(self):
        """生成用户基础数据"""
        print(f"正在生成 {USER_COUNT} 个用户数据...")
        for i in range(1, USER_COUNT + 1):
            user_id = self._id_base["user"] + i
            start_dt = datetime.datetime.combine(SYSTEM_START_DATE, datetime.time(0, 0, 0))
            end_dt = datetime.datetime.combine(SYSTEM_END_DATE, datetime.time(23, 59, 59))
            gmt_create = fake.date_time_between_dates(datetime_start=start_dt, datetime_end=end_dt)
            last_login = fake.date_time_between_dates(datetime_start=gmt_create, datetime_end=end_dt)
            
            user = {
                'id': user_id,
                'username': _clamp_str(fake.user_name() + str(user_id), 64),
                'mobile': _make_mobile(user_id),
                'email': _clamp_str(fake.email(), 128),
                'password_hash': fake.sha256(),
                'salt': fake.uuid4()[:32],
                'nickname': _clamp_str(fake.name(), 64),
                'avatar_url': _clamp_str(fake.image_url(), 512),
                'status': random.choices([1, 2, 3], weights=[80, 15, 5])[0],
                'register_source': random.choice(['APP', 'WEB', 'WX', 'MINIPROGRAM', 'UNKNOWN']),
                'register_ip': fake.ipv4(),
                'last_login_time': _dt_to_mysql(last_login),
                'last_login_ip': fake.ipv4(),
                'gmt_create': _dt_to_mysql(gmt_create),
                'gmt_modified': _dt_to_mysql(last_login),
                'is_deleted': 0,
                'version': 1,
                'level': random.randint(1, 100),
                'total_spend': Decimal(0) # 初始化为 0，后续通过交易累加，保证数据一致性
            }
            self.users.append(user)

            profile = {
                "user_id": user_id,
                "gender": random.choices([0, 1, 2], weights=[20, 40, 40])[0],
                "birthday": fake.date_between(start_date="-60y", end_date="-12y").strftime("%Y-%m-%d"),
                "signature": "",
                "country": "",
                "province": "",
                "city": "",
                "occupation": _clamp_str(random.choice(["学生", "产品经理", "程序员", "设计师", "运营", "厨师", "自由职业", "教师", "医生", "销售"]), 64),
                "interests": "",
                "cook_age": random.randint(0, 20),
                "favorite_cuisine": random.choice(["川菜", "粤菜", "湘菜", "东北菜", "西餐", "烘焙", "日料", "韩餐", "轻食"]),
                "taste_preference": random.choice(["微辣", "中辣", "特辣", "清淡", "甜口", "酸甜", "咸鲜", "重口"]),
                "dietary_restrictions": random.choice(["无", "不吃香菜", "不吃辣", "低糖", "低盐", "素食", "乳糖不耐受", "海鲜过敏"]),
                "vip_level": random.choices([0, 1, 2, 3, 4], weights=[70, 15, 8, 5, 2])[0],
                "vip_expire_time": None,
                "is_master_chef": random.choices([0, 1], weights=[96, 4])[0],
                "master_title": None,
                "bg_image_url": _safe_chaos_str(fake.image_url(), 512),
                "video_intro_url": "",
                "contact_email": _clamp_str(fake.email(), 128),
                "total_spend": Decimal(0),
                "gmt_create": user["gmt_create"],
                "gmt_modified": user["gmt_modified"],
            }
            country, province, city = _build_user_region()
            profile["country"] = country
            profile["province"] = province
            profile["city"] = city
            if profile["vip_level"] > 0:
                expire = fake.date_time_between_dates(
                    datetime_start=datetime.datetime.combine(SYSTEM_START_DATE, datetime.time(0, 0, 0)),
                    datetime_end=datetime.datetime.combine(SYSTEM_END_DATE, datetime.time(23, 59, 59)),
                ) + datetime.timedelta(days=random.randint(30, 540))
                profile["vip_expire_time"] = _dt_to_mysql(expire)
            if profile["is_master_chef"] == 1:
                profile["master_title"] = random.choice(["私房菜主理人", "烘焙达人", "轻食教练", "营养师", "家庭厨神", "料理研究员"])
            profile["interests"] = _build_user_interests(profile["favorite_cuisine"], profile["taste_preference"], profile["dietary_restrictions"])
            profile["signature"] = _clamp_str(
                random.choice([
                    f"爱做{profile['favorite_cuisine']}，偏{profile['taste_preference']}，喜欢{random.choice(profile['interests'].split(',')[:3])}。",
                    f"今天也要好好吃饭。{random.choice(['保持热爱', '慢慢来', '做饭是疗愈', '记录生活', '厨房小白进阶'])}",
                    f"{random.choice(['一人食', '家庭餐', '减脂', '烘焙'])}爱好者，欢迎交流做法。",
                ]),
                255
            )
            self.user_profiles.append(profile)

            stats = {
                "user_id": user_id,
                "level": user["level"],
                "experience": random.randint(0, 800000),
                "total_recipes": 0,
                "total_moments": random.randint(0, 120),
                "total_likes_received": 0,
                "total_collects_received": 0,
                "total_fans": 0,
                "total_follows": 0,
                "total_views": random.randint(0, 200000),
                "week_active_days": random.randint(0, 7),
                "month_active_days": random.randint(0, 30),
                "last_publish_time": None,
                "gmt_modified": user["gmt_modified"],
            }
            self.user_stats.append(stats)

    def generate_transactions(self):
        """生成交易数据，并更新用户的 total_spend 以保持一致性"""
        print(f"正在生成 {TRANS_COUNT} 条交易记录...")
        BIZ_FLOW_MAP = {
            1: 1, # 充值 -> 收入
            2: 2, # 消费 -> 支出
            5: 1  # 佣金 -> 收入
        }

        for i in range(1, TRANS_COUNT + 1):
            user = random.choice(self.users)
            amount = get_pareto_amount()
            biz_type = random.choice([1, 2, 5])
            flow_type = BIZ_FLOW_MAP.get(biz_type, 1)
            
            # 维护一致性：如果是消费，累加到用户的 total_spend
            if biz_type == 2 and flow_type == 2:
                user['total_spend'] += amount

            # 时间逻辑 (增强随机性)
            user_reg_time = datetime.datetime.strptime(user['gmt_create'], "%Y-%m-%d %H:%M:%S.%f")
            if user_reg_time > NOW_DT:
                user_reg_time = NOW_DT - datetime.timedelta(days=1)
            
            # 生成日期
            days_diff = (NOW_DT - user_reg_time).days
            if days_diff < 0: days_diff = 0
            random_days = random.randint(0, days_diff)
            trans_date = user_reg_time + datetime.timedelta(days=random_days)
            
            # 使用时间分布函数
            gmt_create_dt = get_random_time_distribution(trans_date.date())
            gmt_create = gmt_create_dt.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
            stat_date = gmt_create_dt.strftime("%Y-%m-%d")

            trans = {
                'id': self._id_base["transaction"] + i,
                'transaction_no': uuid.uuid4().hex,
                'account_id': user['id'], 
                'biz_order_no': f"ORDER_{uuid.uuid4().hex[:12]}",
                'out_trade_no': f"PAY_{uuid.uuid4().hex[:16]}",
                'user_id': user['id'],
                'account_type': random.randint(1, 4),
                'flow_type': flow_type,
                'biz_type': biz_type,
                'amount': amount,
                'balance_after': round(Decimal(random.uniform(float(amount), 50000)), 2),
                'status': random.choices([0, 1, 2], weights=[10, 85, 5])[0],
                'remark': get_chaos_string(fake.sentence()),
                'gmt_create': gmt_create,
                'gmt_modified': gmt_create
            }
            self.transactions.append(trans)

            # Doris 聚合一致性：每日统计
            if trans['status'] == 1:
                key = (stat_date, user['id'], biz_type) 
                if key not in self.daily_stats:
                    self.daily_stats[key] = {'total_amount': Decimal(0), 'trans_count': 0}
                self.daily_stats[key]['total_amount'] += amount
                self.daily_stats[key]['trans_count'] += 1

    def generate_recipes(self):
        """生成食谱及统计数据"""
        print("正在生成食谱数据 (Recipes)...")
        if not self.users: return

        start_dt = datetime.datetime.combine(SYSTEM_START_DATE, datetime.time(0, 0, 0))
        end_dt = datetime.datetime.combine(SYSTEM_END_DATE, datetime.time(23, 59, 59))
        used_titles = set()
        used_covers = set()
        for i in range(1, RECIPE_COUNT + 1):
            user = random.choice(self.users)
            cat = random.choice(self.categories)
            theme = _build_recipe_theme(cat.get("name") if isinstance(cat, dict) else None)
            gmt_create = fake.date_time_between_dates(datetime_start=start_dt, datetime_end=end_dt)
            status = random.choices([0, 1, 2, 3, 4], weights=[6, 4, 78, 4, 8])[0]
            publish_time = None
            if status == 2:
                publish_time = _rand_dt_between(gmt_create, end_dt)

            title = None
            for _ in range(6):
                t = _build_recipe_title(theme)
                if t not in used_titles:
                    title = t
                    used_titles.add(t)
                    break
                theme["method"] = random.choice(_COOK_METHODS)
                theme["taste"] = list(set((theme.get("taste") or []) + _pick_unique(_TASTE_TAGS, 1)))
            if title is None:
                title = _clamp_str(f"{random.choice(_ADJ)}{theme.get('main')}{random.choice(['做法', '教程', '做起来', '安排上'])}", 128)
                used_titles.add(title)
            
            rcp = {
                'id': self._id_base["recipe"] + i,
                'author_id': user['id'],
                'title': title,
                'cover_url': _clamp_str(_pick_cover_url(used_covers), 512),
                'video_url': "",
                'description': _build_recipe_desc(theme),
                'category_id': cat['id'],
                'cuisine_id': 0,
                'difficulty': random.randint(1, 5),
                'time_cost': random.choice([10, 30, 60, 120]),
                'calories': random.randint(100, 1000),
                'score': round(random.uniform(3.0, 5.0), 1),
                'status': status,
                'gmt_create': _dt_to_mysql(gmt_create),
                'gmt_modified': _dt_to_mysql(gmt_create),
                'is_deleted': 0,
                'version': 1
            }
            rcp["publish_time"] = _dt_to_mysql(publish_time) if publish_time else None
            rcp["ip_location"] = _build_ip_location()
            rcp["device_info"] = _build_device_info()
            rcp["is_exclusive"] = random.choices([0, 1], weights=[90, 10])[0]
            rcp["is_paid"] = random.choices([0, 1], weights=[95, 5])[0]
            rcp["price"] = float(round(Decimal(np.random.lognormal(mean=3.2, sigma=0.45)), 2)) if rcp["is_paid"] == 1 else 0.00
            rcp["tips"] = _build_tip(theme) if random.random() < 0.9 else ""
            rcp["tags"] = json.dumps(_build_recipe_tags(theme), ensure_ascii=False)
            rcp["_theme"] = theme
            self.recipes.append(rcp)
            used_covers.add(rcp["cover_url"])
            
            # 使用 Zipf 分布模拟长尾效应：少数食谱拥有极高热度
            # zipf 参数 > 1, 越小越长尾
            zipf_val = np.random.zipf(a=1.5) 
            base_view = min(zipf_val * 100, 1000000) # 限制最大值
            
            stat = {
                'recipe_id': rcp['id'],
                'view_count': base_view,
                'like_count': 0,
                'collect_count': 0,
                'comment_count': 0,
                'share_count': 0,
                'try_count': int(base_view * random.uniform(0.0001, 0.001)),
                'score': rcp['score'],
                'gmt_modified': rcp['gmt_modified']
            }
            self.stats.append(stat)

    def generate_recipe_steps(self):
        if not self.recipes:
            return
        step_id = self._id_base["step"]
        end_dt = datetime.datetime.combine(SYSTEM_END_DATE, datetime.time(23, 59, 59))
        for r in self.recipes:
            theme = r.get("_theme") or _build_recipe_theme(None)
            pool = theme.get("pool") or "家常菜"
            base = 7
            if pool == "烘焙":
                base = 8
            elif pool == "饮品":
                base = 6
            elif pool == "西餐":
                base = 7
            n = int(max(3, min(int(np.random.lognormal(mean=1.5, sigma=0.5)) + base - 3, 18)))
            r["_step_count"] = n
            step_texts = _build_recipe_steps(theme, n)
            base_dt = datetime.datetime.strptime(r["gmt_create"], "%Y-%m-%d %H:%M:%S.%f")
            for i in range(1, n + 1):
                step_id += 1
                desc = step_texts[i - 1] if i - 1 < len(step_texts) else step_texts[-1]
                img_url = fake.image_url() if random.random() < 0.45 else None
                video_url = fake.image_url() if random.random() < 0.06 else None
                gmt_create = _rand_dt_between(base_dt, end_dt)
                self.recipe_steps.append(
                    {
                        "id": step_id,
                        "recipe_id": r["id"],
                        "step_no": i,
                        "desc": desc,
                        "img_url": img_url,
                        "video_url": video_url,
                        "time_cost": random.choice([0, 1, 2, 3, 5, 8, 10, 12, 15]),
                        "is_key_step": 1 if random.random() < 0.18 else 0,
                        "voice_url": None,
                        "gmt_create": _dt_to_mysql(gmt_create),
                    }
                )
        self._id_base["step"] = step_id

    def generate_follows(self):
        if not self.users:
            return
        user_ids = [u["id"] for u in self.users]
        weights = np.random.zipf(a=1.35, size=len(user_ids)).astype(float)
        weights = weights / weights.sum()
        edges = set()
        follows = []
        start_dt = datetime.datetime.combine(SYSTEM_START_DATE, datetime.time(0, 0, 0))
        end_dt = datetime.datetime.combine(SYSTEM_END_DATE, datetime.time(23, 59, 59))
        target_edges = max(0, int(FOLLOW_EDGE_TARGET))
        attempts = 0
        while len(edges) < target_edges and attempts < target_edges * 12:
            attempts += 1
            follower_id = random.choice(user_ids)
            following_id = int(np.random.choice(user_ids, p=weights))
            if follower_id == following_id:
                continue
            key = (follower_id, following_id)
            if key in edges:
                continue
            edges.add(key)
            gmt_create = fake.date_time_between_dates(datetime_start=start_dt, datetime_end=end_dt)
            gmt_modified = _rand_dt_between(gmt_create, end_dt)
            follows.append(
                {
                    "follower_id": follower_id,
                    "following_id": following_id,
                    "status": random.choices([1, 0], weights=[92, 8])[0],
                    "gmt_create": _dt_to_mysql(gmt_create),
                    "gmt_modified": _dt_to_mysql(gmt_modified),
                }
            )
        self.follows = follows

    def generate_comments_and_interactions(self):
        if not self.recipes or not self.users:
            return
        user_ids = [u["id"] for u in self.users]
        stat_by_recipe = {s["recipe_id"]: s for s in self.stats}
        recipe_by_id = {r["id"]: r for r in self.recipes}

        interaction_id = self._id_base["interaction"]
        comment_id = self._id_base["comment"]
        interactions = []
        comments = []

        for r in self.recipes:
            s = stat_by_recipe.get(r["id"])
            if not s:
                continue
            theme = r.get("_theme") or _build_recipe_theme(None)
            step_count = int(r.get("_step_count") or 0)
            view_count = int(s["view_count"])
            base_dt = datetime.datetime.strptime(r["publish_time"] or r["gmt_create"], "%Y-%m-%d %H:%M:%S.%f")
            end_dt = datetime.datetime.combine(SYSTEM_END_DATE, datetime.time(23, 59, 59))

            like_target = min(int(view_count * random.uniform(0.02, 0.09)), MAX_RECIPE_INTERACTIONS_PER_RECIPE)
            collect_target = min(int(view_count * random.uniform(0.004, 0.03)), MAX_RECIPE_INTERACTIONS_PER_RECIPE // 2)
            share_target = min(int(view_count * random.uniform(0.001, 0.015)), MAX_RECIPE_INTERACTIONS_PER_RECIPE // 4)
            root_comment_target = min(int(view_count * random.uniform(0.0008, 0.006)), MAX_RECIPE_COMMENTS_PER_RECIPE)

            like_users = set()
            collect_users = set()
            share_users = set()

            def pick_unique_users(target, banned_user_id=None):
                picked = set()
                loops = 0
                while len(picked) < target and loops < target * 20:
                    loops += 1
                    uid = random.choice(user_ids)
                    if banned_user_id is not None and uid == banned_user_id:
                        continue
                    picked.add(uid)
                return picked

            like_users = pick_unique_users(like_target, banned_user_id=r["author_id"])
            collect_users = pick_unique_users(collect_target, banned_user_id=r["author_id"])
            share_users = pick_unique_users(share_target, banned_user_id=None)

            for uid in like_users:
                interaction_id += 1
                gmt_create = _dt_to_mysql(_rand_dt_between(base_dt, end_dt))
                interactions.append((interaction_id, uid, 1, r["id"], 1, None, gmt_create))

            for uid in collect_users:
                interaction_id += 1
                gmt_create = _dt_to_mysql(_rand_dt_between(base_dt, end_dt))
                interactions.append((interaction_id, uid, 1, r["id"], 2, None, gmt_create))

            for uid in share_users:
                interaction_id += 1
                gmt_create = _dt_to_mysql(_rand_dt_between(base_dt, end_dt))
                interactions.append((interaction_id, uid, 1, r["id"], 3, None, gmt_create))

            root_comments = []
            for _ in range(root_comment_target):
                comment_id += 1
                uid = random.choice(user_ids)
                gmt_create_dt = _rand_dt_between(base_dt, end_dt)
                content = _build_comment_text(theme, step_count, as_reply=False)
                img_urls = None
                if random.random() < 0.12:
                    img_urls = json.dumps([fake.image_url() for _ in range(random.randint(1, 3))], ensure_ascii=False)
                root_id = comment_id
                parent_id = 0
                like_count = int(min(np.random.zipf(a=1.7) - 1, 500))
                comments.append((comment_id, uid, 1, r["id"], content, img_urls, parent_id, root_id, like_count, _dt_to_mysql(gmt_create_dt)))
                root_comments.append(comment_id)

            for root_cid in root_comments:
                reply_n = int(min(np.random.zipf(a=2.3) - 1, 10))
                for _ in range(reply_n):
                    comment_id += 1
                    uid = random.choice(user_ids)
                    gmt_create_dt = _rand_dt_between(base_dt, end_dt)
                    content = _build_comment_text(theme, step_count, as_reply=True)
                    parent_id = random.choice([root_cid] + [c[0] for c in comments[-min(len(comments), 30):]])
                    like_count = int(min(np.random.zipf(a=2.0) - 1, 120))
                    comments.append((comment_id, uid, 1, r["id"], content, None, parent_id, root_cid, like_count, _dt_to_mysql(gmt_create_dt)))

            s["like_count"] = len(like_users)
            s["collect_count"] = len(collect_users)
            s["share_count"] = len(share_users)
            total_comment_count = len([c for c in comments if c[3] == r["id"]])
            s["comment_count"] = total_comment_count

            r["like_count"] = s["like_count"]
            r["collect_count"] = s["collect_count"]
            r["share_count"] = s["share_count"]
            r["comment_count"] = total_comment_count
            r["view_count"] = view_count

        self._id_base["interaction"] = interaction_id
        self._id_base["comment"] = comment_id
        self.interactions = interactions
        self.comments = comments

    def generate_points_records(self):
        if not self.users:
            return
        user_by_id = {u["id"]: u for u in self.users}
        recipe_by_id = {r["id"]: r for r in self.recipes}

        points = []
        end_dt = datetime.datetime.combine(SYSTEM_END_DATE, datetime.time(23, 59, 59))
        signin_start = (NOW_DT - datetime.timedelta(days=SIGNIN_LOOKBACK_DAYS)).date()
        signin_start_dt = datetime.datetime.combine(signin_start, datetime.time(0, 0, 0))

        for u in self.users:
            n = int(max(0, min(np.random.poisson(lam=55), 320)))
            days = set()
            loops = 0
            while len(days) < n and loops < n * 20:
                loops += 1
                dt = fake.date_time_between_dates(datetime_start=signin_start_dt, datetime_end=end_dt)
                days.add(dt.date())
            for d in days:
                gmt = get_random_time_distribution(d)
                amount = random.choice([1, 2, 3, 5, 8, 10, 12])
                points.append((u["id"], 1, amount, "签到", _dt_to_mysql(gmt)))

            exch_n = int(min(np.random.zipf(a=2.6) - 1, 6))
            for _ in range(exch_n):
                gmt = fake.date_time_between_dates(datetime_start=signin_start_dt, datetime_end=end_dt)
                amount = -random.choice([50, 80, 100, 120, 150, 200, 260, 320, 500])
                points.append((u["id"], 10, amount, "积分兑换商品", _dt_to_mysql(gmt)))

        for r in self.recipes:
            if r.get("status") != 2:
                continue
            publish_dt = datetime.datetime.strptime(r["publish_time"] or r["gmt_create"], "%Y-%m-%d %H:%M:%S.%f")
            points.append((r["author_id"], 2, 50, "发布食谱", _dt_to_mysql(publish_dt)))

        like_gain = {}
        for (iid, uid, target_type, target_id, action_type, device_id, gmt_create) in self.interactions:
            if target_type != 1 or action_type != 1:
                continue
            recipe = recipe_by_id.get(target_id)
            if not recipe:
                continue
            author_id = recipe["author_id"]
            if author_id == uid:
                continue
            if random.random() > 0.12:
                continue
            day = gmt_create.split(" ")[0]
            key = (author_id, day)
            like_gain[key] = like_gain.get(key, 0) + 1
        for (author_id, day), amt in like_gain.items():
            dt = datetime.datetime.strptime(day + " 12:00:00.000", "%Y-%m-%d %H:%M:%S.%f")
            points.append((author_id, 3, int(amt), "被点赞", _dt_to_mysql(dt)))

        self.points_records = points

    def generate_products_orders(self):
        if not self.users:
            return
        base_product_id = self._id_base["product"]
        base_order_id = self._id_base["order"]
        base_order_item_id = self._id_base["order_item"]

        start_dt = datetime.datetime.combine(SYSTEM_START_DATE, datetime.time(0, 0, 0))
        end_dt = datetime.datetime.combine(SYSTEM_END_DATE, datetime.time(23, 59, 59))
        user_ids = [u["id"] for u in self.users]

        brands = ["苏泊尔", "美的", "九阳", "小熊", "松下", "飞利浦", "德龙", "WMF", "双立人", "康宁", "乐扣", "摩飞", "小米", "KitchenAid"]
        materials = ["304不锈钢", "铸铁", "陶瓷", "钛合金", "麦饭石", "高硼硅玻璃", "食品级硅胶"]
        sizes = ["20cm", "24cm", "26cm", "28cm", "30cm", "32cm"]
        watt = ["600W", "800W", "1000W", "1200W", "1500W"]
        product_types = [
            ("不粘锅", 1), ("炒锅", 1), ("汤锅", 1), ("砂锅", 1), ("刀具套装", 1), ("砧板", 1),
            ("空气炸锅", 2), ("烤箱", 2), ("电饭煲", 2), ("电磁炉", 2), ("料理机", 2), ("破壁机", 2), ("厨师机", 2),
            ("烘焙模具", 3), ("电子秤", 3), ("打蛋器", 3), ("量杯量勺", 3), ("裱花袋", 3), ("硅胶刮刀", 3),
            ("零基础家常菜入门课", 4), ("烘焙进阶课程", 4), ("咖啡手冲训练营", 4),
            ("围裙", 5), ("贴纸套装", 5), ("餐具周边", 5),
        ]

        products = []
        used_titles = set()
        for i in range(1, EXTRA_PRODUCT_COUNT + 1):
            pid = base_product_id + i
            ptype, category_id = random.choice(product_types)
            brand = random.choice(brands)
            model = f"{random.choice(['A', 'B', 'C', 'X', 'Z'])}{random.randint(10, 99)}-{random.randint(100, 999)}"
            spec = ""
            if ptype in ("不粘锅", "炒锅", "汤锅", "砂锅"):
                spec = f"{random.choice(sizes)}/{random.choice(materials)}"
            elif ptype in ("空气炸锅", "烤箱", "电饭煲", "电磁炉", "料理机", "破壁机", "厨师机"):
                spec = f"{random.choice(watt)}/{random.choice(['家用', '轻商用'])}"
            elif category_id == 4:
                spec = random.choice(["30天打卡", "体系课", "速成课", "进阶课"])
            elif category_id == 5:
                spec = random.choice(["经典款", "限定款", "加厚款", "礼盒装"])
            else:
                spec = random.choice(["入门套装", "进阶套装", "专业版", "家庭版"])

            base_title = f"{brand} {ptype} {spec} {model}"
            title = _clamp_str(base_title, 128)
            tries = 0
            while title in used_titles and tries < 8:
                tries += 1
                model = f"{random.choice(['A', 'B', 'C', 'X', 'Z'])}{random.randint(10, 99)}-{random.randint(100, 999)}"
                spec = spec if random.random() < 0.7 else f"{random.choice(sizes)}/{random.choice(materials)}"
                title = _clamp_str(f"{brand} {ptype} {spec} {model}", 128)
            used_titles.add(title)

            price = float(round(Decimal(np.random.lognormal(mean=4.45, sigma=0.65)), 2))
            if category_id == 3:
                price = float(round(Decimal(np.random.lognormal(mean=3.65, sigma=0.55)), 2))
            stock = int(max(0, min(np.random.pareto(a=1.35) * 120, 5000)))
            gmt_create = fake.date_time_between_dates(datetime_start=start_dt, datetime_end=end_dt)
            gmt_modified = _rand_dt_between(gmt_create, end_dt)
            desc = _clamp_str(
                random.choice([
                    f"{ptype} {spec}，做工扎实，适合日常高频使用。",
                    f"{brand} 出品，{ptype} 易清洁，适合{random.choice(['煎炒', '烘焙', '炖煮', '蒸煮'])}场景。",
                    f"升级材质 {random.choice(materials)}，细节到位，兼顾颜值与实用。",
                    "支持多场景使用，入门到进阶都适配。",
                ]),
                80
            )
            products.append((pid, title, desc, price, stock, category_id, _dt_to_mysql(gmt_create), _dt_to_mysql(gmt_modified)))

        orders = []
        order_items = []

        product_pool = products[:]
        if len(product_pool) == 0:
            product_pool = [(base_product_id + i, f"商品{i}", "", float(random.randint(9, 399)), 100, 1, _dt_to_mysql(start_dt), _dt_to_mysql(start_dt)) for i in range(1, 10)]

        for i in range(1, ORDER_COUNT + 1):
            oid = base_order_id + i
            user_id = random.choice(user_ids)
            status = random.choices([0, 1, 2, 3, 4], weights=[16, 20, 14, 36, 14])[0]
            gmt_create = fake.date_time_between_dates(datetime_start=start_dt, datetime_end=end_dt)
            pay_time = None
            gmt_modified = gmt_create
            if status in (1, 2, 3):
                pay_time = gmt_create + datetime.timedelta(minutes=random.randint(1, 240))
                gmt_modified = pay_time + datetime.timedelta(hours=random.randint(1, 240))
            elif status == 4:
                gmt_modified = gmt_create + datetime.timedelta(minutes=random.randint(1, 240))

            item_n = random.randint(1, 5)
            chosen = random.sample(product_pool, k=min(item_n, len(product_pool)))
            total = Decimal(0)
            for p in chosen:
                base_order_item_id += 1
                cnt = random.randint(1, 3)
                price = Decimal(str(p[3])) * Decimal(str(random.uniform(0.85, 1.02)))
                price = Decimal(price).quantize(Decimal("0.01"))
                total += price * cnt
                order_items.append((base_order_item_id, oid, p[0], float(price), cnt))

            orders.append((oid, user_id, float(total), status, _dt_to_mysql(pay_time) if pay_time else None, _dt_to_mysql(gmt_create), _dt_to_mysql(gmt_modified)))

        self._id_base["product"] = base_product_id + EXTRA_PRODUCT_COUNT
        self._id_base["order"] = base_order_id + ORDER_COUNT
        self._id_base["order_item"] = base_order_item_id
        self.products = products
        self.orders = orders
        self.order_items = order_items

    def finalize_consistency(self):
        if self.user_profiles:
            user_total_spend = {u["id"]: u.get("total_spend", Decimal(0)) for u in self.users}
            for p in self.user_profiles:
                p["total_spend"] = user_total_spend.get(p["user_id"], Decimal(0))

        if self.user_stats:
            by_user = {s["user_id"]: s for s in self.user_stats}

            recipes_by_author = {}
            for r in self.recipes:
                recipes_by_author.setdefault(r["author_id"], []).append(r)

            for uid, rs in recipes_by_author.items():
                st = by_user.get(uid)
                if not st:
                    continue
                st["total_recipes"] = len(rs)
                publish_times = [r.get("publish_time") for r in rs if r.get("publish_time")]
                st["last_publish_time"] = max(publish_times) if publish_times else st.get("last_publish_time")
                st["total_likes_received"] = int(sum(int(r.get("like_count") or 0) for r in rs))
                st["total_collects_received"] = int(sum(int(r.get("collect_count") or 0) for r in rs))

            if self.follows:
                fans = {}
                follows = {}
                for f in self.follows:
                    if int(f.get("status") or 0) != 1:
                        continue
                    fans[f["following_id"]] = fans.get(f["following_id"], 0) + 1
                    follows[f["follower_id"]] = follows.get(f["follower_id"], 0) + 1
                for uid, st in by_user.items():
                    st["total_fans"] = int(fans.get(uid, 0))
                    st["total_follows"] = int(follows.get(uid, 0))

    def _execute_sql_batch(self, conn, sql, data, batch_size=100):
        """辅助方法：批量执行 SQL"""
        try:
            with conn.cursor() as cursor:
                for i in range(0, len(data), batch_size):
                    batch = data[i:i+batch_size]
                    cursor.executemany(sql, batch)
            conn.commit()
        except Exception as e:
            print(f"SQL 执行错误: {e}")
            conn.rollback()

    def sync_to_mysql(self):
        """直接连接 MySQL 并插入数据"""
        if not MYSQL_AVAILABLE: return
        print("正在同步数据到 MySQL...")
        try:
            conn = pymysql.connect(**MYSQL_CONFIG)
            
            db_name = MYSQL_CONFIG.get("db")
            table_usr_base = _mysql_table_exists(conn, "t_usr_base", db_name)
            table_usr_profile = _mysql_table_exists(conn, "t_usr_profile", db_name)
            table_usr_stats = _mysql_table_exists(conn, "t_usr_stats", db_name)
            table_rcp_category = _mysql_table_exists(conn, "t_rcp_category", db_name)
            table_rcp_info = _mysql_table_exists(conn, "t_rcp_info", db_name)
            table_rcp_stats = _mysql_table_exists(conn, "t_rcp_stats", db_name)
            table_rcp_step = _mysql_table_exists(conn, "t_rcp_step", db_name)
            table_soc_follow = _mysql_table_exists(conn, "t_soc_follow", db_name)
            table_soc_comment = _mysql_table_exists(conn, "t_soc_comment", db_name)
            table_soc_interaction = _mysql_table_exists(conn, "t_soc_interaction", db_name)
            table_points_record = _mysql_table_exists(conn, "t_points_record", db_name)
            table_comm_product = _mysql_table_exists(conn, "t_comm_product", db_name)
            table_comm_order = _mysql_table_exists(conn, "t_comm_order", db_name)
            table_comm_order_item = _mysql_table_exists(conn, "t_comm_order_item", db_name)
            table_fin_flow = _mysql_table_exists(conn, "t_fin_transaction_flow", db_name)

            if APPEND_MODE:
                self.set_id_base(
                    user=_mysql_max_id(conn, "t_usr_base", "id") if table_usr_base else 0,
                    recipe=_mysql_max_id(conn, "t_rcp_info", "id") if table_rcp_info else 10000,
                    step=_mysql_max_id(conn, "t_rcp_step", "id") if table_rcp_step else 0,
                    interaction=_mysql_max_id(conn, "t_soc_interaction", "id") if table_soc_interaction else 0,
                    transaction=_mysql_max_id(conn, "t_fin_transaction_flow", "id") if table_fin_flow else 0,
                    comment=_mysql_max_id(conn, "t_soc_comment", "id") if table_soc_comment else 0,
                    product=_mysql_max_id(conn, "t_comm_product", "id") if table_comm_product else 0,
                    order=_mysql_max_id(conn, "t_comm_order", "id") if table_comm_order else 0,
                    order_item=_mysql_max_id(conn, "t_comm_order_item", "id") if table_comm_order_item else 0,
                )

            # 1. t_usr_base
            print("  - 写入 t_usr_base...")
            if table_usr_base:
                sql = """
                    INSERT INTO t_usr_base (id, username, mobile, email, password_hash, salt, nickname, avatar_url, status, register_source, register_ip, last_login_time, last_login_ip, gmt_create, gmt_modified, is_deleted, version) 
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """
                data = [(u['id'], u['username'], u['mobile'], u['email'], u['password_hash'], u['salt'], u['nickname'], u['avatar_url'], u['status'], u['register_source'], u['register_ip'], u['last_login_time'], u['last_login_ip'], u['gmt_create'], u['gmt_modified'], u['is_deleted'], u['version']) for u in self.users]
                self._execute_sql_batch(conn, sql, data)
            else:
                print("    跳过: t_usr_base 不存在（请确保已执行 mysql.sql 初始化）")
            
            # 1.1 t_usr_profile
            print("  - 写入 t_usr_profile...")
            if table_usr_profile:
                sql = """
                    INSERT INTO t_usr_profile (user_id, gender, birthday, signature, country, province, city, occupation, interests, cook_age, favorite_cuisine, taste_preference, dietary_restrictions, vip_expire_time, vip_level, is_master_chef, master_title, bg_image_url, video_intro_url, contact_email, total_spend, gmt_create, gmt_modified)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                      gender=VALUES(gender),
                      birthday=VALUES(birthday),
                      signature=VALUES(signature),
                      country=VALUES(country),
                      province=VALUES(province),
                      city=VALUES(city),
                      occupation=VALUES(occupation),
                      interests=VALUES(interests),
                      cook_age=VALUES(cook_age),
                      favorite_cuisine=VALUES(favorite_cuisine),
                      taste_preference=VALUES(taste_preference),
                      dietary_restrictions=VALUES(dietary_restrictions),
                      vip_expire_time=VALUES(vip_expire_time),
                      vip_level=VALUES(vip_level),
                      is_master_chef=VALUES(is_master_chef),
                      master_title=VALUES(master_title),
                      bg_image_url=VALUES(bg_image_url),
                      video_intro_url=VALUES(video_intro_url),
                      contact_email=VALUES(contact_email),
                      total_spend=VALUES(total_spend),
                      gmt_modified=VALUES(gmt_modified)
                """
                data = [
                    (
                        p["user_id"],
                        p["gender"],
                        p["birthday"],
                        p["signature"],
                        p["country"],
                        p["province"],
                        p["city"],
                        p["occupation"],
                        p["interests"],
                        p["cook_age"],
                        p["favorite_cuisine"],
                        p["taste_preference"],
                        p["dietary_restrictions"],
                        p["vip_expire_time"],
                        p["vip_level"],
                        p["is_master_chef"],
                        p["master_title"],
                        p["bg_image_url"],
                        p["video_intro_url"],
                        p["contact_email"],
                        p["total_spend"],
                        p["gmt_create"],
                        p["gmt_modified"],
                    )
                    for p in self.user_profiles
                ]
                self._execute_sql_batch(conn, sql, data)
            else:
                print("    跳过: t_usr_profile 不存在（请确保已执行 mysql.sql 初始化）")

            # 1.2 t_usr_stats
            print("  - 写入 t_usr_stats...")
            if table_usr_stats:
                sql = """
                    INSERT INTO t_usr_stats (user_id, level, experience, total_recipes, total_moments, total_likes_received, total_collects_received, total_fans, total_follows, total_views, week_active_days, month_active_days, last_publish_time, gmt_modified)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                      level=VALUES(level),
                      experience=VALUES(experience),
                      total_recipes=VALUES(total_recipes),
                      total_moments=VALUES(total_moments),
                      total_likes_received=VALUES(total_likes_received),
                      total_collects_received=VALUES(total_collects_received),
                      total_fans=VALUES(total_fans),
                      total_follows=VALUES(total_follows),
                      total_views=VALUES(total_views),
                      week_active_days=VALUES(week_active_days),
                      month_active_days=VALUES(month_active_days),
                      last_publish_time=VALUES(last_publish_time),
                      gmt_modified=VALUES(gmt_modified)
                """
                data = [
                    (
                        s["user_id"],
                        s["level"],
                        s["experience"],
                        s["total_recipes"],
                        s["total_moments"],
                        s["total_likes_received"],
                        s["total_collects_received"],
                        s["total_fans"],
                        s["total_follows"],
                        s["total_views"],
                        s["week_active_days"],
                        s["month_active_days"],
                        s["last_publish_time"],
                        s["gmt_modified"],
                    )
                    for s in self.user_stats
                ]
                self._execute_sql_batch(conn, sql, data)
            else:
                print("    跳过: t_usr_stats 不存在（请确保已执行 mysql.sql 初始化）")

            # 2. t_rcp_category
            print("  - 写入 t_rcp_category...")
            if table_rcp_category:
                sql = "INSERT INTO t_rcp_category (id, name, parent_id, level, sort, is_visible) VALUES (%s, %s, %s, %s, %s, 1) ON DUPLICATE KEY UPDATE name=VALUES(name), parent_id=VALUES(parent_id), level=VALUES(level), sort=VALUES(sort), is_visible=VALUES(is_visible)"
                data = [(c['id'], c['name'], c['parent_id'], c['level'], c['sort']) for c in self.categories]
                self._execute_sql_batch(conn, sql, data)
            else:
                print("    跳过: t_rcp_category 不存在（请确保已执行 mysql.sql 初始化）")

            # 3. t_rcp_info
            print("  - 写入 t_rcp_info...")
            if table_rcp_info:
                sql = """
                    INSERT INTO t_rcp_info (id, author_id, title, cover_url, video_url, description, category_id, cuisine_id, difficulty, time_cost, calories, score, view_count, like_count, collect_count, comment_count, share_count, try_count, status, tags, tips, is_exclusive, is_paid, price, publish_time, ip_location, device_info, gmt_create, gmt_modified, is_deleted, version)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """
                data = []
                for r in self.recipes:
                    s = next((x for x in self.stats if x['recipe_id'] == r['id']), None)
                    v_count = int(s['view_count']) if s else 0
                    like_count = int(s['like_count']) if s else 0
                    collect_count = int(s['collect_count']) if s else 0
                    comment_count = int(r.get("comment_count", 0))
                    share_count = int(s['share_count']) if s else 0
                    tags_json = r.get("tags") or None
                    if isinstance(tags_json, (list, tuple)):
                        tags_json = json.dumps(list(tags_json), ensure_ascii=False)
                    data.append((
                        r['id'],
                        r['author_id'],
                        r['title'],
                        r['cover_url'],
                        r.get('video_url') or None,
                        r.get('description') or None,
                        r['category_id'],
                        r.get('cuisine_id') or 0,
                        r.get('difficulty') or 1,
                        r.get('time_cost') or 0,
                        r.get('calories') or 0,
                        r.get('score') or 0,
                        v_count,
                        like_count,
                        collect_count,
                        comment_count,
                        share_count,
                        int(s['try_count']) if s else 0,
                        r.get('status') or 0,
                        tags_json,
                        r.get('tips') or None,
                        r.get('is_exclusive') or 0,
                        r.get('is_paid') or 0,
                        r.get('price') or 0.00,
                        r.get('publish_time'),
                        r.get('ip_location'),
                        r.get('device_info'),
                        r['gmt_create'],
                        r['gmt_modified'],
                        r['is_deleted'],
                        r['version']
                    ))
                self._execute_sql_batch(conn, sql, data)
            else:
                print("    跳过: t_rcp_info 不存在（请确保已执行 mysql.sql 初始化）")

            # 4. t_rcp_stats
            print("  - 写入 t_rcp_stats...")
            if table_rcp_stats:
                sql = "INSERT INTO t_rcp_stats (recipe_id, view_count, like_count, collect_count, comment_count, share_count, try_count, score, gmt_modified) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)"
                data = [(s['recipe_id'], s['view_count'], s['like_count'], s['collect_count'], s['comment_count'], s['share_count'], s['try_count'], s['score'], s['gmt_modified']) for s in self.stats]
                self._execute_sql_batch(conn, sql, data)
            else:
                print("    跳过: t_rcp_stats 不存在（请确保已执行 mysql.sql 初始化）")

            # 4.1 t_rcp_step
            if self.recipe_steps:
                print("  - 写入 t_rcp_step...")
                if table_rcp_step:
                    sql = """
                        INSERT INTO t_rcp_step (id, recipe_id, step_no, `desc`, img_url, video_url, time_cost, is_key_step, voice_url, gmt_create)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """
                    data = [
                        (
                            x["id"],
                            x["recipe_id"],
                            x["step_no"],
                            x["desc"],
                            x["img_url"],
                            x["video_url"],
                            x["time_cost"],
                            x["is_key_step"],
                            x["voice_url"],
                            x["gmt_create"],
                        )
                        for x in self.recipe_steps
                    ]
                    self._execute_sql_batch(conn, sql, data, batch_size=200)
                else:
                    print("    跳过: t_rcp_step 不存在（请确保已执行 mysql.sql 初始化）")

            # 4.2 t_soc_follow
            if self.follows:
                print("  - 写入 t_soc_follow...")
                if table_soc_follow:
                    sql = """
                        INSERT IGNORE INTO t_soc_follow (follower_id, following_id, status, gmt_create, gmt_modified)
                        VALUES (%s, %s, %s, %s, %s)
                    """
                    data = [(f["follower_id"], f["following_id"], f["status"], f["gmt_create"], f["gmt_modified"]) for f in self.follows]
                    self._execute_sql_batch(conn, sql, data, batch_size=200)
                else:
                    print("    跳过: t_soc_follow 不存在（可执行 sql/mysql.sql 的 2.6.1 或 fix_tables_final.sql 补齐）")

            # 4.3 t_soc_comment
            if self.comments:
                print("  - 写入 t_soc_comment...")
                if table_soc_comment:
                    sql = """
                        INSERT INTO t_soc_comment (id, user_id, target_type, target_id, content, img_urls, parent_id, root_id, like_count, gmt_create)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """
                    self._execute_sql_batch(conn, sql, self.comments, batch_size=200)
                else:
                    print("    跳过: t_soc_comment 不存在（请确保已执行 mysql.sql 初始化）")

            # 4.4 t_soc_interaction
            if self.interactions:
                print("  - 写入 t_soc_interaction...")
                if table_soc_interaction:
                    sql = """
                        INSERT IGNORE INTO t_soc_interaction (id, user_id, target_type, target_id, action_type, device_id, gmt_create)
                        VALUES (%s, %s, %s, %s, %s, %s, %s)
                    """
                    self._execute_sql_batch(conn, sql, self.interactions, batch_size=300)
                else:
                    print("    跳过: t_soc_interaction 不存在（请确保已执行 mysql.sql 初始化）")

            # 4.5 t_points_record
            if self.points_records:
                print("  - 写入 t_points_record...")
                if table_points_record:
                    sql = """
                        INSERT INTO t_points_record (user_id, type, amount, description, gmt_create)
                        VALUES (%s, %s, %s, %s, %s)
                    """
                    self._execute_sql_batch(conn, sql, self.points_records, batch_size=300)
                else:
                    print("    跳过: t_points_record 不存在（可执行 sql/mysql.sql 的 2.6.2 或 fix_tables_final.sql 补齐）")

            # 4.6 t_comm_product / t_comm_order / t_comm_order_item
            if self.products:
                print("  - 写入 t_comm_product...")
                if table_comm_product:
                    sql = """
                        INSERT INTO t_comm_product (id, title, description, price, stock, category_id, gmt_create, gmt_modified)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                    """
                    self._execute_sql_batch(conn, sql, self.products, batch_size=200)
                else:
                    print("    跳过: t_comm_product 不存在（可执行 sql/mysql.sql 的 2.6.3 补齐）")
            if self.orders:
                print("  - 写入 t_comm_order...")
                if table_comm_order:
                    sql = """
                        INSERT INTO t_comm_order (id, user_id, total_amount, status, pay_time, gmt_create, gmt_modified)
                        VALUES (%s, %s, %s, %s, %s, %s, %s)
                    """
                    self._execute_sql_batch(conn, sql, self.orders, batch_size=200)
                else:
                    print("    跳过: t_comm_order 不存在（可执行 sql/mysql.sql 的 2.6.4 补齐）")
            if self.order_items:
                print("  - 写入 t_comm_order_item...")
                if table_comm_order_item:
                    sql = """
                        INSERT INTO t_comm_order_item (id, order_id, product_id, price, count)
                        VALUES (%s, %s, %s, %s, %s)
                    """
                    self._execute_sql_batch(conn, sql, self.order_items, batch_size=300)
                else:
                    print("    跳过: t_comm_order_item 不存在（可执行 sql/mysql.sql 的 2.6.5 补齐）")

            # 5. t_fin_transaction_flow
            print("  - 写入 t_fin_transaction_flow...")
            if table_fin_flow:
                sql = """
                    INSERT INTO t_fin_transaction_flow (id, transaction_no, account_id, biz_order_no, out_trade_no, user_id, account_type, flow_type, biz_type, amount, balance_after, status, remark, gmt_create, gmt_modified)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """
                data = [(t['id'], t['transaction_no'], t['account_id'], t['biz_order_no'], t['out_trade_no'], t['user_id'], t['account_type'], t['flow_type'], t['biz_type'], t['amount'], t['balance_after'], t['status'], t['remark'], t['gmt_create'], t['gmt_modified']) for t in self.transactions]
                self._execute_sql_batch(conn, sql, data)
            else:
                print("    跳过: t_fin_transaction_flow 不存在（请确保已执行 mysql.sql 初始化）")

            conn.close()
            print("MySQL 数据同步完成。")
        except Exception as e:
            print(f"MySQL 同步失败: {e}")

    def sync_to_doris(self):
        """直接连接 Doris 并插入数据 (使用 MySQL 协议)"""
        if not MYSQL_AVAILABLE: return
        print("正在同步数据到 Doris...")
        if not _can_connect(DORIS_CONFIG["host"], DORIS_CONFIG["port"]):
            print(f"Doris 同步失败: 无法连接到 {DORIS_CONFIG['host']}:{DORIS_CONFIG['port']} (端口未监听/未映射/服务未启动)")
            return
        try:
            conn = pymysql.connect(**DORIS_CONFIG)
            
            # 1. dim_user_profile
            print("  - 写入 dim_user_profile...")
            sql = "INSERT INTO dim_user_profile (user_id, nickname, level, reg_time, last_login_time, total_spend) VALUES (%s, %s, %s, %s, %s, %s)"
            data = [(u['id'], u['nickname'], u['level'], u['gmt_create'], u['last_login_time'], u['total_spend']) for u in self.users]
            self._execute_sql_batch(conn, sql, data)

            # 2. dws_fin_daily_settlement
            print("  - 写入 dws_fin_daily_settlement...")
            sql = "INSERT INTO dws_fin_daily_settlement (stat_date, merchant_id, biz_type, total_amount, trans_count) VALUES (%s, %s, %s, %s, %s)"
            data = []
            for (date, merchant_id, biz_type), stats in self.daily_stats.items():
                data.append((date, merchant_id, biz_type, stats['total_amount'], stats['trans_count']))
            self._execute_sql_batch(conn, sql, data)

            # 3. dws_recipe_daily_stats
            print("  - 写入 dws_recipe_daily_stats...")
            sql = "INSERT INTO dws_recipe_daily_stats (stat_date, recipe_id, category_id, view_count, like_count, comment_count, share_count) VALUES (%s, %s, %s, %s, %s, %s, %s)"
            data = []
            for r in self.recipes:
                s = next((x for x in self.stats if x['recipe_id'] == r['id']), None)
                if s:
                    stat_date = s['gmt_modified'].split(' ')[0]
                    # 为了演示，生成一些随机的当日增量数据
                    data.append((stat_date, r['id'], r['category_id'], 
                                 random.randint(0, s['view_count']), 
                                 random.randint(0, s['like_count']), 
                                 random.randint(0, s['comment_count']), 
                                 random.randint(0, s['share_count'])))
            self._execute_sql_batch(conn, sql, data)

            conn.close()
            print("Doris 数据同步完成。")
        except Exception as e:
            print(f"Doris 同步失败: {e} (请确保 Doris 服务已启动且表结构已创建)")

    def sync_to_es(self):
        """同步食谱数据到 Elasticsearch"""
        print("正在同步食谱数据到 Elasticsearch...")
        host, port = _parse_url_host_port(ES_HOST, 9200)
        if not _can_connect(host, port):
            print(f"无法连接到 ES ({ES_HOST})，跳过同步。")
            return

        index_name = os.getenv("CW_ES_INDEX", "recipe_index")
        if RESET_ES_INDEX:
            try:
                req = urllib.request.Request(url=f"{ES_HOST}/{index_name}", method="DELETE")
                urllib.request.urlopen(req, timeout=8).read()
                print(f"ES 已删除索引: {index_name}")
            except urllib.error.HTTPError as e:
                if e.code != 404:
                    try:
                        msg = e.read().decode("utf-8", errors="ignore")
                    except Exception:
                        msg = str(e)
                    print(f"ES 删除索引失败: HTTP {e.code} {msg}")
            except Exception as e:
                print(f"ES 删除索引失败: {e}")

        def build_doc(r):
            return {
                "id": r["id"],
                "title": r["title"],
                "description": r.get("description") or "",
                "authorId": r["author_id"],
                "status": r.get("status") or 0,
                "createTime": r.get("gmt_create"),
            }


        bulk_lines = []
        for r in self.recipes:
            bulk_lines.append(json.dumps({"index": {"_index": index_name, "_id": r["id"]}}, ensure_ascii=False))
        payload = ("\n".join(bulk_lines) + "\n").encode("utf-8")

        req = urllib.request.Request(
            url=f"{ES_HOST}/_bulk",
            data=payload,
            headers={"Content-Type": "application/x-ndjson"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                body = resp.read().decode("utf-8", errors="ignore")
            data = json.loads(body) if body else {}
            errors = bool(data.get("errors"))
            items = data.get("items") or []
            ok = sum(1 for it in items if (it.get("index") or {}).get("status") in (200, 201))
            fail = len(items) - ok
            if errors:
                print(f"ES 同步完成但存在失败: 成功 {ok}，失败 {fail}（可检查 ES 日志/映射）")
            else:
                print(f"ES 同步结果: 成功索引 {ok} 条文档。")
        except urllib.error.HTTPError as e:
            try:
                msg = e.read().decode("utf-8", errors="ignore")
            except Exception:
                msg = str(e)
            print(f"ES 批量操作错误: HTTP {e.code} {msg}")
        except Exception as e:
            print(f"ES 批量操作错误: {e}")

    def write_sql_files(self):
        """将生成的数据写入 SQL 文件（离线模式）"""
        print(f"正在写入 SQL 文件: {MYSQL_FILE} ...")
        
        # 辅助函数：格式化值为 SQL 字符串
        def fmt(v):
            if v is None:
                return "NULL"
            if isinstance(v, (int, float, Decimal)):
                return str(v)
            # 转义单引号
            s = str(v).replace("'", "''").replace("\\", "\\\\")
            return f"'{s}'"

        with open(MYSQL_FILE, "w", encoding="utf-8") as f:
            f.write("-- Auto-generated by chaos_data_gen.py\n")
            f.write("SET NAMES utf8mb4;\n\n")

            # 1. t_usr_base
            if self.users:
                f.write("-- t_usr_base\n")
                values = []
                for u in self.users:
                    row = [
                        u['id'], u['username'], u['mobile'], u['email'], u['password_hash'], 
                        u['salt'], u['nickname'], u['avatar_url'], u['status'], u['register_source'], 
                        u['register_ip'], u['last_login_time'], u['last_login_ip'], u['gmt_create'], 
                        u['gmt_modified'], u['is_deleted'], u['version']
                    ]
                    values.append(f"({', '.join(map(fmt, row))})")
                
                # 分批写入
                batch_size = 500
                for i in range(0, len(values), batch_size):
                    batch = values[i:i+batch_size]
                    sql = f"INSERT INTO t_usr_base (id, username, mobile, email, password_hash, salt, nickname, avatar_url, status, register_source, register_ip, last_login_time, last_login_ip, gmt_create, gmt_modified, is_deleted, version) VALUES \n{',\n'.join(batch)};\n"
                    f.write(sql)
                f.write("\n")

            # 1.1 t_usr_profile
            if self.user_profiles:
                f.write("-- t_usr_profile\n")
                values = []
                for p in self.user_profiles:
                    row = [
                        p["user_id"], p["gender"], p["birthday"], p["signature"], p["country"], 
                        p["province"], p["city"], p["occupation"], p["interests"], p["cook_age"], 
                        p["favorite_cuisine"], p["taste_preference"], p["dietary_restrictions"], 
                        p["vip_expire_time"], p["vip_level"], p["is_master_chef"], p["master_title"], 
                        p["bg_image_url"], p["video_intro_url"], p["contact_email"], p["total_spend"], 
                        p["gmt_create"], p["gmt_modified"]
                    ]
                    values.append(f"({', '.join(map(fmt, row))})")
                
                for i in range(0, len(values), batch_size):
                    batch = values[i:i+batch_size]
                    sql = f"INSERT INTO t_usr_profile (user_id, gender, birthday, signature, country, province, city, occupation, interests, cook_age, favorite_cuisine, taste_preference, dietary_restrictions, vip_expire_time, vip_level, is_master_chef, master_title, bg_image_url, video_intro_url, contact_email, total_spend, gmt_create, gmt_modified) VALUES \n{',\n'.join(batch)} \nON DUPLICATE KEY UPDATE gmt_modified=VALUES(gmt_modified);\n"
                    f.write(sql)
                f.write("\n")

            # 1.2 t_usr_stats
            if self.user_stats:
                f.write("-- t_usr_stats\n")
                values = []
                for s in self.user_stats:
                    row = [
                        s["user_id"], s["level"], s["experience"], s["total_recipes"], s["total_moments"], 
                        s["total_likes_received"], s["total_collects_received"], s["total_fans"], 
                        s["total_follows"], s["total_views"], s["week_active_days"], s["month_active_days"], 
                        s["last_publish_time"], s["gmt_modified"]
                    ]
                    values.append(f"({', '.join(map(fmt, row))})")
                
                for i in range(0, len(values), batch_size):
                    batch = values[i:i+batch_size]
                    sql = f"INSERT INTO t_usr_stats (user_id, level, experience, total_recipes, total_moments, total_likes_received, total_collects_received, total_fans, total_follows, total_views, week_active_days, month_active_days, last_publish_time, gmt_modified) VALUES \n{',\n'.join(batch)} \nON DUPLICATE KEY UPDATE gmt_modified=VALUES(gmt_modified);\n"
                    f.write(sql)
                f.write("\n")

            # 2. t_rcp_category
            if self.categories:
                f.write("-- t_rcp_category\n")
                values = []
                for c in self.categories:
                    row = [c['id'], c['name'], c['parent_id'], c['level'], c['sort'], 1]
                    values.append(f"({', '.join(map(fmt, row))})")
                sql = f"INSERT INTO t_rcp_category (id, name, parent_id, level, sort, is_visible) VALUES \n{',\n'.join(values)} \nON DUPLICATE KEY UPDATE name=VALUES(name);\n"
                f.write(sql)
                f.write("\n")

            # 3. t_rcp_info
            if self.recipes:
                f.write("-- t_rcp_info\n")
                values = []
                for r in self.recipes:
                    s = next((x for x in self.stats if x['recipe_id'] == r['id']), None)
                    v_count = int(s['view_count']) if s else 0
                    like_count = int(s['like_count']) if s else 0
                    collect_count = int(s['collect_count']) if s else 0
                    comment_count = int(r.get("comment_count", 0))
                    share_count = int(s['share_count']) if s else 0
                    try_count = int(s['try_count']) if s else 0
                    tags_json = r.get("tags")
                    if isinstance(tags_json, (list, tuple)):
                        tags_json = json.dumps(list(tags_json), ensure_ascii=False)
                    
                    row = [
                        r['id'], r['author_id'], r['title'], r['cover_url'], r.get('video_url'), 
                        r.get('description'), r['category_id'], r.get('cuisine_id', 0), 
                        r.get('difficulty', 1), r.get('time_cost', 0), r.get('calories', 0), 
                        r.get('score', 0), v_count, like_count, collect_count, comment_count, 
                        share_count, try_count, r.get('status', 0), tags_json, r.get('tips'), 
                        r.get('is_exclusive', 0), r.get('is_paid', 0), r.get('price', 0), 
                        r.get('publish_time'), r.get('ip_location'), r.get('device_info'), 
                        r['gmt_create'], r['gmt_modified'], r['is_deleted'], r['version']
                    ]
                    values.append(f"({', '.join(map(fmt, row))})")
                
                for i in range(0, len(values), batch_size):
                    batch = values[i:i+batch_size]
                    sql = f"INSERT INTO t_rcp_info (id, author_id, title, cover_url, video_url, description, category_id, cuisine_id, difficulty, time_cost, calories, score, view_count, like_count, collect_count, comment_count, share_count, try_count, status, tags, tips, is_exclusive, is_paid, price, publish_time, ip_location, device_info, gmt_create, gmt_modified, is_deleted, version) VALUES \n{',\n'.join(batch)};\n"
                    f.write(sql)
                f.write("\n")

            # 4. t_rcp_stats
            if self.stats:
                f.write("-- t_rcp_stats\n")
                values = []
                for s in self.stats:
                    row = [
                        s['recipe_id'], s['view_count'], s['like_count'], s['collect_count'], 
                        s['comment_count'], s['share_count'], s['try_count'], s['score'], s['gmt_modified']
                    ]
                    values.append(f"({', '.join(map(fmt, row))})")
                
                for i in range(0, len(values), batch_size):
                    batch = values[i:i+batch_size]
                    sql = f"INSERT INTO t_rcp_stats (recipe_id, view_count, like_count, collect_count, comment_count, share_count, try_count, score, gmt_modified) VALUES \n{',\n'.join(batch)};\n"
                    f.write(sql)
                f.write("\n")
            
            # 4.1 t_rcp_step
            if self.recipe_steps:
                f.write("-- t_rcp_step\n")
                values = []
                for x in self.recipe_steps:
                    row = [
                        x["id"], x["recipe_id"], x["step_no"], x["desc"], x["img_url"], 
                        x["video_url"], x["time_cost"], x["is_key_step"], x["voice_url"], x["gmt_create"]
                    ]
                    values.append(f"({', '.join(map(fmt, row))})")
                
                for i in range(0, len(values), batch_size):
                    batch = values[i:i+batch_size]
                    sql = f"INSERT INTO t_rcp_step (id, recipe_id, step_no, `desc`, img_url, video_url, time_cost, is_key_step, voice_url, gmt_create) VALUES \n{',\n'.join(batch)};\n"
                    f.write(sql)
                f.write("\n")

            # 4.2 t_soc_follow
            if self.follows:
                f.write("-- t_soc_follow\n")
                values = []
                for follow in self.follows:
                    row = [follow["follower_id"], follow["following_id"], follow["status"], follow["gmt_create"], follow["gmt_modified"]]
                    values.append(f"({', '.join(map(fmt, row))})")
                
                for i in range(0, len(values), batch_size):
                    batch = values[i:i+batch_size]
                    sql = f"INSERT IGNORE INTO t_soc_follow (follower_id, following_id, status, gmt_create, gmt_modified) VALUES \n{',\n'.join(batch)};\n"
                    f.write(sql)
                f.write("\n")

            # 4.3 t_soc_comment
            if self.comments:
                f.write("-- t_soc_comment\n")
                values = []
                for c in self.comments:
                    # c is tuple: (id, user_id, target_type, target_id, content, img_urls, parent_id, root_id, like_count, gmt_create)
                    row = list(c)
                    values.append(f"({', '.join(map(fmt, row))})")
                
                for i in range(0, len(values), batch_size):
                    batch = values[i:i+batch_size]
                    sql = f"INSERT INTO t_soc_comment (id, user_id, target_type, target_id, content, img_urls, parent_id, root_id, like_count, gmt_create) VALUES \n{',\n'.join(batch)};\n"
                    f.write(sql)
                f.write("\n")

            # 4.4 t_soc_interaction
            if self.interactions:
                f.write("-- t_soc_interaction\n")
                values = []
                for x in self.interactions:
                    row = list(x)
                    values.append(f"({', '.join(map(fmt, row))})")
                
                for i in range(0, len(values), batch_size):
                    batch = values[i:i+batch_size]
                    sql = f"INSERT IGNORE INTO t_soc_interaction (id, user_id, target_type, target_id, action_type, device_id, gmt_create) VALUES \n{',\n'.join(batch)};\n"
                    f.write(sql)
                f.write("\n")
            
            # 4.5 t_points_record
            if self.points_records:
                f.write("-- t_points_record\n")
                values = []
                for x in self.points_records:
                    row = list(x)
                    values.append(f"({', '.join(map(fmt, row))})")
                
                for i in range(0, len(values), batch_size):
                    batch = values[i:i+batch_size]
                    sql = f"INSERT INTO t_points_record (user_id, type, amount, description, gmt_create) VALUES \n{',\n'.join(batch)};\n"
                    f.write(sql)
                f.write("\n")
            
            # 4.6 t_comm_product
            if self.products:
                f.write("-- t_comm_product\n")
                values = []
                for x in self.products:
                    row = list(x)
                    values.append(f"({', '.join(map(fmt, row))})")
                
                for i in range(0, len(values), batch_size):
                    batch = values[i:i+batch_size]
                    sql = f"INSERT INTO t_comm_product (id, title, description, price, stock, category_id, gmt_create, gmt_modified) VALUES \n{',\n'.join(batch)};\n"
                    f.write(sql)
                f.write("\n")
            
            # 4.7 t_comm_order
            if self.orders:
                f.write("-- t_comm_order\n")
                values = []
                for x in self.orders:
                    row = list(x)
                    values.append(f"({', '.join(map(fmt, row))})")
                
                for i in range(0, len(values), batch_size):
                    batch = values[i:i+batch_size]
                    sql = f"INSERT INTO t_comm_order (id, user_id, total_amount, status, pay_time, gmt_create, gmt_modified) VALUES \n{',\n'.join(batch)};\n"
                    f.write(sql)
                f.write("\n")
            
            # 4.8 t_comm_order_item
            if self.order_items:
                f.write("-- t_comm_order_item\n")
                values = []
                for x in self.order_items:
                    row = list(x)
                    values.append(f"({', '.join(map(fmt, row))})")
                
                for i in range(0, len(values), batch_size):
                    batch = values[i:i+batch_size]
                    sql = f"INSERT INTO t_comm_order_item (id, order_id, product_id, price, count) VALUES \n{',\n'.join(batch)};\n"
                    f.write(sql)
                f.write("\n")

            # 5. t_fin_transaction_flow
            if self.transactions:
                f.write("-- t_fin_transaction_flow\n")
                values = []
                for t in self.transactions:
                    row = [
                        t['id'], t['transaction_no'], t['account_id'], t['biz_order_no'], t['out_trade_no'], 
                        t['user_id'], t['account_type'], t['flow_type'], t['biz_type'], t['amount'], 
                        t['balance_after'], t['status'], t['remark'], t['gmt_create'], t['gmt_modified']
                    ]
                    values.append(f"({', '.join(map(fmt, row))})")
                
                for i in range(0, len(values), batch_size):
                    batch = values[i:i+batch_size]
                    sql = f"INSERT INTO t_fin_transaction_flow (id, transaction_no, account_id, biz_order_no, out_trade_no, user_id, account_type, flow_type, biz_type, amount, balance_after, status, remark, gmt_create, gmt_modified) VALUES \n{',\n'.join(batch)};\n"
                    f.write(sql)
                f.write("\n")

        print(f"SQL 文件写入完成: {MYSQL_FILE}")

if __name__ == "__main__":
    print("=== 开始执行混沌数据生成与同步任务 ===")
    
    # 尝试重置数据库（如果在离线模式或连接失败，这里会报错或被跳过，需要处理异常）
    if RESET_DB:
        try:
            reset_mysql_schema_and_seed()
        except Exception as e:
            print(f"数据库重置失败: {e}，将继续执行数据生成（可能生成 SQL 文件）")

    factory = DataFactory()

    # 尝试连接数据库获取最大 ID，如果失败则使用默认值
    if MYSQL_AVAILABLE and APPEND_MODE:
        try:
            conn = pymysql.connect(**MYSQL_CONFIG)
            db_name = MYSQL_CONFIG.get("db")
            def max_or_zero(tbl, default_zero=0):
                return _mysql_max_id(conn, tbl, "id") if _mysql_table_exists(conn, tbl, db_name) else default_zero
            factory.set_id_base(
                user=max_or_zero("t_usr_base", 0),
                recipe=max_or_zero("t_rcp_info", 10000),
                step=max_or_zero("t_rcp_step", 0),
                interaction=max_or_zero("t_soc_interaction", 0),
                transaction=max_or_zero("t_fin_transaction_flow", 0),
                comment=max_or_zero("t_soc_comment", 0),
                product=max_or_zero("t_comm_product", 0),
                order=max_or_zero("t_comm_order", 0),
                order_item=max_or_zero("t_comm_order_item", 0),
            )
            conn.close()
        except Exception as e:
            print(f"警告: 无法读取现有库的 MAX(id) 或连接数据库失败: {e}")
            print("将使用默认 ID 起点。如果目标数据库非空，导入 SQL 可能会主键冲突。")
    
    # 1. 生成内存数据
    factory.generate_users()
    factory.generate_categories()
    factory.generate_recipes()
    factory.generate_recipe_steps()
    factory.generate_follows()
    factory.generate_comments_and_interactions()
    factory.generate_points_records()
    factory.generate_products_orders()
    factory.generate_transactions()
    factory.finalize_consistency()
    
    # 2. 同步到各个数据源
    # 优先尝试直连同步，如果失败则写入 SQL 文件
    sync_success = False
    try:
        factory.sync_to_mysql()
        sync_success = True
    except Exception as e:
        print(f"MySQL 同步过程中出错: {e}")
    
    if not sync_success or not _can_connect(MYSQL_CONFIG["host"], MYSQL_CONFIG["port"]):
        print("检测到无法连接 MySQL，转为写入 SQL 文件...")
        factory.write_sql_files()

    factory.sync_to_doris()
    factory.sync_to_es()
    
    print("=== 所有任务完成 ===")
