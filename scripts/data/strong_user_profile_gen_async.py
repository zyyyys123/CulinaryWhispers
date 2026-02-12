import argparse
import asyncio
import datetime
import json
import random
import uuid
from dataclasses import dataclass
from decimal import Decimal
from typing import Dict, List, Optional, Sequence, Tuple

try:
    import faker
except ImportError as e:
    raise SystemExit("缺少依赖 faker，请先安装：pip install faker") from e

try:
    import numpy as np
except ImportError as e:
    raise SystemExit("缺少依赖 numpy，请先安装：pip install numpy") from e

try:
    import pymysql
    MYSQL_AVAILABLE = True
except ImportError:
    MYSQL_AVAILABLE = False


fake = faker.Faker(["zh_CN", "en_US", "ja_JP"])


@dataclass
class MysqlConfig:
    host: str
    port: int
    user: str
    password: str
    db: str
    charset: str = "utf8mb4"


CN_GEO: Sequence[Tuple[str, Sequence[str]]] = [
    ("北京市", ["北京市"]),
    ("上海市", ["上海市"]),
    ("广东省", ["广州市", "深圳市", "佛山市", "东莞市", "珠海市"]),
    ("浙江省", ["杭州市", "宁波市", "温州市", "绍兴市", "金华市"]),
    ("江苏省", ["南京市", "苏州市", "无锡市", "常州市", "南通市"]),
    ("四川省", ["成都市", "绵阳市", "德阳市", "乐山市", "宜宾市"]),
    ("湖北省", ["武汉市", "宜昌市", "襄阳市", "荆州市"]),
    ("山东省", ["济南市", "青岛市", "烟台市", "潍坊市"]),
    ("福建省", ["福州市", "厦门市", "泉州市", "漳州市"]),
    ("湖南省", ["长沙市", "株洲市", "湘潭市", "岳阳市"]),
]

INTEREST_POOL = [
    "烘焙", "家常菜", "西餐", "饮品", "健身", "跑步", "摄影", "旅行", "咖啡", "茶",
    "露营", "极简", "撸猫", "动漫", "美食探店", "火锅", "烧烤", "日料", "甜品", "轻食"
]

TASTE_POOL = ["清淡", "微辣", "中辣", "重辣", "甜口", "咸鲜", "酸甜", "麻辣", "蒜香", "孜然"]
RESTRICT_POOL = ["花生", "乳糖", "海鲜", "牛肉", "鸡蛋", "麸质", "香菜", "葱姜蒜", "高糖", "高油"]
OCCUPATION_POOL = ["学生", "程序员", "设计师", "产品经理", "教师", "医生", "运营", "会计", "厨师", "自由职业"]


def _rand_choice_weighted(items: Sequence[str], weights: Sequence[float]) -> str:
    return random.choices(items, weights=weights, k=1)[0]


def _random_mobile(used: set) -> str:
    while True:
        mobile = "1" + "".join(str(random.randint(0, 9)) for _ in range(10))
        if mobile not in used:
            used.add(mobile)
            return mobile


def _random_username(used: set, idx: int) -> str:
    while True:
        base = fake.user_name()
        suffix = f"{idx}_{uuid.uuid4().hex[:6]}"
        username = f"{base}_{suffix}".lower()
        username = "".join(ch for ch in username if ch.isalnum() or ch in ("_",))
        if 6 <= len(username) <= 64 and username not in used:
            used.add(username)
            return username


def _random_email(used: set, username: str) -> str:
    domains = ["example.com", "mail.com", "qq.com", "163.com", "gmail.com", "outlook.com"]
    while True:
        local = username[:20]
        email = f"{local}.{uuid.uuid4().hex[:4]}@{random.choice(domains)}"
        if email not in used:
            used.add(email)
            return email


def _random_datetime_between(start: datetime.datetime, end: datetime.datetime) -> datetime.datetime:
    if start >= end:
        return start
    delta = end - start
    sec = random.randint(0, int(delta.total_seconds()))
    return start + datetime.timedelta(seconds=sec, milliseconds=random.randint(0, 999))


def _ms(dt: datetime.datetime) -> str:
    return dt.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]


def _pareto_money(scale: float = 30.0, alpha: float = 1.3, cap: float = 500000.0) -> Decimal:
    val = (np.random.pareto(alpha) + 1) * scale
    val = float(min(val, cap))
    return Decimal(str(round(val, 2)))


def _zipf_int(base: int, a: float, cap: int) -> int:
    v = int(np.random.zipf(a))
    return min(base * v, cap)


def _pick_city() -> Tuple[str, str, str]:
    province, cities = random.choice(list(CN_GEO))
    city = random.choice(list(cities))
    return "中国", province, city


def _pick_interests() -> str:
    n = random.randint(3, 8)
    return ",".join(random.sample(INTEREST_POOL, k=n))


def _pick_taste() -> str:
    n = random.randint(1, 3)
    return ",".join(random.sample(TASTE_POOL, k=n))


def _pick_restrictions() -> str:
    if random.random() < 0.55:
        return ""
    n = random.randint(1, 3)
    return ",".join(random.sample(RESTRICT_POOL, k=n))


def _rand_ip() -> str:
    return ".".join(str(random.randint(1, 254)) for _ in range(4))


def _maybe(value, p: float = 0.3):
    return value if random.random() < p else None


def _gender() -> int:
    return random.choices([0, 1, 2], weights=[12, 44, 44], k=1)[0]


def generate_users(user_count: int, seed_user_id_start: int = 1) -> Tuple[List[Dict], List[Dict], List[Dict]]:
    now = datetime.datetime.now()
    start = now - datetime.timedelta(days=365 * 3)

    used_username = set()
    used_mobile = set()
    used_email = set()

    users: List[Dict] = []
    profiles: List[Dict] = []
    stats_list: List[Dict] = []

    for i in range(user_count):
        user_id = seed_user_id_start + i
        reg = _random_datetime_between(start, now)
        last_login = _random_datetime_between(reg, now)

        username = _random_username(used_username, user_id)
        mobile = _random_mobile(used_mobile)
        email = _random_email(used_email, username)

        status = random.choices([1, 2, 3], weights=[86, 12, 2], k=1)[0]
        register_source = _rand_choice_weighted(
            ["APP", "WEB", "WX", "MINIPROGRAM", "UNKNOWN"],
            [0.38, 0.32, 0.18, 0.10, 0.02]
        )

        nickname = fake.name()
        avatar_url = f"https://api.dicebear.com/7.x/avataaars/svg?seed={user_id}"

        users.append({
            "id": user_id,
            "username": username,
            "mobile": mobile,
            "email": email,
            "password_hash": fake.sha256(),
            "salt": uuid.uuid4().hex[:32],
            "nickname": nickname,
            "avatar_url": avatar_url,
            "status": status,
            "register_source": register_source,
            "register_ip": _rand_ip(),
            "last_login_time": _ms(last_login),
            "last_login_ip": _rand_ip(),
            "gmt_create": _ms(reg),
            "gmt_modified": _ms(last_login),
            "is_deleted": 0,
            "version": 1
        })

        country, province, city = _pick_city()
        cook_age = int(np.clip(np.random.normal(loc=2.0, scale=2.5), 0, 30))
        vip_level = int(np.clip(np.random.exponential(scale=0.8), 0, 10))
        is_master_chef = 1 if random.random() < 0.035 else 0
        master_title = None
        if is_master_chef:
            master_title = random.choice(["金牌大厨", "私房料理师", "烘焙达人", "营养搭配师", "粤菜高手", "川味大师"])

        total_spend = _pareto_money(scale=60.0, alpha=1.2, cap=800000.0) if random.random() < 0.7 else _pareto_money(scale=10.0, alpha=1.8, cap=50000.0)

        profiles.append({
            "user_id": user_id,
            "gender": _gender(),
            "birthday": _maybe((reg.date() - datetime.timedelta(days=random.randint(18 * 365, 45 * 365))).strftime("%Y-%m-%d"), 0.85),
            "signature": _maybe(fake.sentence(nb_words=random.randint(6, 14)), 0.75),
            "region_code": _maybe(str(random.randint(110000, 659999)), 0.6),
            "country": country,
            "province": province,
            "city": city,
            "real_name": _maybe(fake.name(), 0.35),
            "id_card_no": _maybe(uuid.uuid4().hex, 0.2),
            "occupation": _maybe(random.choice(OCCUPATION_POOL), 0.8),
            "interests": _pick_interests(),
            "cook_age": cook_age,
            "favorite_cuisine": _maybe(random.choice(["川菜", "粤菜", "湘菜", "西餐", "日料", "烘焙", "轻食"]), 0.9),
            "taste_preference": _pick_taste(),
            "dietary_restrictions": _pick_restrictions(),
            "vip_expire_time": _maybe(_ms(now + datetime.timedelta(days=int(np.random.exponential(scale=180)))), 0.35),
            "vip_level": vip_level,
            "wechat_openid": _maybe(uuid.uuid4().hex[:24], 0.18),
            "wechat_unionid": _maybe(uuid.uuid4().hex[:24], 0.12),
            "weibo_uid": _maybe(str(random.randint(10**8, 10**10)), 0.10),
            "tiktok_uid": _maybe(str(random.randint(10**8, 10**10)), 0.12),
            "is_master_chef": is_master_chef,
            "master_title": master_title,
            "bg_image_url": _maybe("https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1800&q=80", 0.55),
            "video_intro_url": _maybe("https://www.youtube.com/watch?v=dQw4w9WgXcQ", 0.05),
            "contact_email": _maybe(email, 0.35),
            "total_spend": str(total_spend),
            "gmt_create": _ms(reg),
            "gmt_modified": _ms(last_login)
        })

        level = int(np.clip(np.random.lognormal(mean=1.1, sigma=0.6), 1, 30))
        experience = int(np.clip(np.random.lognormal(mean=4.2, sigma=0.9), 0, 50000))
        stats_list.append({
            "user_id": user_id,
            "level": level,
            "experience": experience,
            "total_recipes": _zipf_int(base=1, a=1.6, cap=200),
            "total_moments": _zipf_int(base=1, a=1.8, cap=500),
            "total_likes_received": _zipf_int(base=3, a=1.7, cap=500000),
            "total_collects_received": _zipf_int(base=2, a=1.8, cap=200000),
            "total_fans": _zipf_int(base=1, a=1.9, cap=80000),
            "total_follows": _zipf_int(base=1, a=2.0, cap=5000),
            "total_views": _zipf_int(base=5, a=1.6, cap=2000000),
            "week_active_days": random.randint(0, 7),
            "month_active_days": random.randint(0, 30),
            "last_publish_time": _maybe(_ms(_random_datetime_between(reg, now)), 0.6),
            "gmt_modified": _ms(now)
        })

    return users, profiles, stats_list


def _sql_escape(s: Optional[str]) -> str:
    if s is None:
        return "NULL"
    return "'" + s.replace("\\", "\\\\").replace("'", "''") + "'"


def _sql_num(n) -> str:
    if n is None:
        return "NULL"
    return str(n)


def write_sql_file(path: str, users: List[Dict], profiles: List[Dict], stats_list: List[Dict]) -> None:
    lines: List[str] = []
    lines.append("USE culinary_user;")
    lines.append("")

    for u in users:
        cols = [
            "id", "username", "mobile", "email", "password_hash", "salt", "nickname", "avatar_url",
            "status", "register_source", "register_ip", "last_login_time", "last_login_ip",
            "gmt_create", "gmt_modified", "is_deleted", "version"
        ]
        values = [
            _sql_num(u["id"]),
            _sql_escape(u["username"]),
            _sql_escape(u["mobile"]),
            _sql_escape(u["email"]),
            _sql_escape(u["password_hash"]),
            _sql_escape(u["salt"]),
            _sql_escape(u["nickname"]),
            _sql_escape(u["avatar_url"]),
            _sql_num(u["status"]),
            _sql_escape(u["register_source"]),
            _sql_escape(u["register_ip"]),
            _sql_escape(u["last_login_time"]),
            _sql_escape(u["last_login_ip"]),
            _sql_escape(u["gmt_create"]),
            _sql_escape(u["gmt_modified"]),
            _sql_num(u["is_deleted"]),
            _sql_num(u["version"]),
        ]
        lines.append(f"INSERT INTO t_usr_base ({', '.join(cols)}) VALUES ({', '.join(values)});")

    lines.append("")

    for p in profiles:
        cols = [
            "user_id", "gender", "birthday", "signature", "region_code", "country", "province", "city", "real_name",
            "id_card_no", "occupation", "interests", "cook_age", "favorite_cuisine", "taste_preference",
            "dietary_restrictions", "vip_expire_time", "vip_level", "wechat_openid", "wechat_unionid", "weibo_uid",
            "tiktok_uid", "is_master_chef", "master_title", "bg_image_url", "video_intro_url", "contact_email",
            "total_spend", "gmt_create", "gmt_modified"
        ]
        values = [
            _sql_num(p["user_id"]),
            _sql_num(p["gender"]),
            _sql_escape(p["birthday"]),
            _sql_escape(p["signature"]),
            _sql_escape(p["region_code"]),
            _sql_escape(p["country"]),
            _sql_escape(p["province"]),
            _sql_escape(p["city"]),
            _sql_escape(p["real_name"]),
            _sql_escape(p["id_card_no"]),
            _sql_escape(p["occupation"]),
            _sql_escape(p["interests"]),
            _sql_num(p["cook_age"]),
            _sql_escape(p["favorite_cuisine"]),
            _sql_escape(p["taste_preference"]),
            _sql_escape(p["dietary_restrictions"]),
            _sql_escape(p["vip_expire_time"]),
            _sql_num(p["vip_level"]),
            _sql_escape(p["wechat_openid"]),
            _sql_escape(p["wechat_unionid"]),
            _sql_escape(p["weibo_uid"]),
            _sql_escape(p["tiktok_uid"]),
            _sql_num(p["is_master_chef"]),
            _sql_escape(p["master_title"]),
            _sql_escape(p["bg_image_url"]),
            _sql_escape(p["video_intro_url"]),
            _sql_escape(p["contact_email"]),
            _sql_num(p["total_spend"]),
            _sql_escape(p["gmt_create"]),
            _sql_escape(p["gmt_modified"]),
        ]
        lines.append(f"INSERT INTO t_usr_profile ({', '.join(cols)}) VALUES ({', '.join(values)});")

    lines.append("")

    for s in stats_list:
        cols = [
            "user_id", "level", "experience", "total_recipes", "total_moments", "total_likes_received",
            "total_collects_received", "total_fans", "total_follows", "total_views", "week_active_days",
            "month_active_days", "last_publish_time", "gmt_modified"
        ]
        values = [
            _sql_num(s["user_id"]),
            _sql_num(s["level"]),
            _sql_num(s["experience"]),
            _sql_num(s["total_recipes"]),
            _sql_num(s["total_moments"]),
            _sql_num(s["total_likes_received"]),
            _sql_num(s["total_collects_received"]),
            _sql_num(s["total_fans"]),
            _sql_num(s["total_follows"]),
            _sql_num(s["total_views"]),
            _sql_num(s["week_active_days"]),
            _sql_num(s["month_active_days"]),
            _sql_escape(s["last_publish_time"]),
            _sql_escape(s["gmt_modified"]),
        ]
        lines.append(f"INSERT INTO t_usr_stats ({', '.join(cols)}) VALUES ({', '.join(values)});")

    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")


def _execute_sql_batch(conn, sql: str, data: List[Tuple], batch_size: int = 200) -> None:
    with conn.cursor() as cursor:
        for i in range(0, len(data), batch_size):
            cursor.executemany(sql, data[i:i + batch_size])
    conn.commit()


def sync_to_mysql(cfg: MysqlConfig, users: List[Dict], profiles: List[Dict], stats_list: List[Dict]) -> None:
    if not MYSQL_AVAILABLE:
        raise SystemExit("缺少依赖 pymysql，无法直连 MySQL。请先安装：pip install pymysql")
    conn = pymysql.connect(
        host=cfg.host,
        port=cfg.port,
        user=cfg.user,
        password=cfg.password,
        db=cfg.db,
        charset=cfg.charset
    )
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT DATABASE()")
            db_name = cursor.fetchone()[0]
            cursor.execute(
                "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=%s AND TABLE_NAME=%s AND COLUMN_NAME=%s",
                (db_name, "t_usr_profile", "total_spend")
            )
            exists = cursor.fetchone()[0]
            if not exists:
                cursor.execute("ALTER TABLE t_usr_profile ADD COLUMN total_spend DECIMAL(12,2) DEFAULT 0.00 COMMENT '用户总消费金额'")
                conn.commit()

        sql = """
            INSERT INTO t_usr_base (
              id, username, mobile, email, password_hash, salt, nickname, avatar_url, status,
              register_source, register_ip, last_login_time, last_login_ip, gmt_create, gmt_modified, is_deleted, version
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
        """
        data = [
            (
                u["id"], u["username"], u["mobile"], u["email"], u["password_hash"], u["salt"], u["nickname"], u["avatar_url"],
                u["status"], u["register_source"], u["register_ip"], u["last_login_time"], u["last_login_ip"],
                u["gmt_create"], u["gmt_modified"], u["is_deleted"], u["version"]
            )
            for u in users
        ]
        _execute_sql_batch(conn, sql, data)

        sql = """
            INSERT INTO t_usr_profile (
              user_id, gender, birthday, signature, region_code, country, province, city, real_name, id_card_no,
              occupation, interests, cook_age, favorite_cuisine, taste_preference, dietary_restrictions,
              vip_expire_time, vip_level, wechat_openid, wechat_unionid, weibo_uid, tiktok_uid,
              is_master_chef, master_title, bg_image_url, video_intro_url, contact_email, total_spend, gmt_create, gmt_modified
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
            ON DUPLICATE KEY UPDATE
              gender=VALUES(gender),
              birthday=VALUES(birthday),
              signature=VALUES(signature),
              region_code=VALUES(region_code),
              country=VALUES(country),
              province=VALUES(province),
              city=VALUES(city),
              real_name=VALUES(real_name),
              id_card_no=VALUES(id_card_no),
              occupation=VALUES(occupation),
              interests=VALUES(interests),
              cook_age=VALUES(cook_age),
              favorite_cuisine=VALUES(favorite_cuisine),
              taste_preference=VALUES(taste_preference),
              dietary_restrictions=VALUES(dietary_restrictions),
              vip_expire_time=VALUES(vip_expire_time),
              vip_level=VALUES(vip_level),
              wechat_openid=VALUES(wechat_openid),
              wechat_unionid=VALUES(wechat_unionid),
              weibo_uid=VALUES(weibo_uid),
              tiktok_uid=VALUES(tiktok_uid),
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
                p["user_id"], p["gender"], p["birthday"], p["signature"], p["region_code"], p["country"], p["province"], p["city"],
                p["real_name"], p["id_card_no"], p["occupation"], p["interests"], p["cook_age"], p["favorite_cuisine"],
                p["taste_preference"], p["dietary_restrictions"], p["vip_expire_time"], p["vip_level"], p["wechat_openid"],
                p["wechat_unionid"], p["weibo_uid"], p["tiktok_uid"], p["is_master_chef"], p["master_title"], p["bg_image_url"],
                p["video_intro_url"], p["contact_email"], p["total_spend"], p["gmt_create"], p["gmt_modified"]
            )
            for p in profiles
        ]
        _execute_sql_batch(conn, sql, data)

        sql = """
            INSERT INTO t_usr_stats (
              user_id, level, experience, total_recipes, total_moments, total_likes_received, total_collects_received,
              total_fans, total_follows, total_views, week_active_days, month_active_days, last_publish_time, gmt_modified
            ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
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
                s["user_id"], s["level"], s["experience"], s["total_recipes"], s["total_moments"], s["total_likes_received"],
                s["total_collects_received"], s["total_fans"], s["total_follows"], s["total_views"], s["week_active_days"],
                s["month_active_days"], s["last_publish_time"], s["gmt_modified"]
            )
            for s in stats_list
        ]
        _execute_sql_batch(conn, sql, data)
    finally:
        conn.close()


async def main_async() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--count", type=int, default=200)
    parser.add_argument("--user-id-start", type=int, default=1000)
    parser.add_argument("--write-sql", type=str, default="sql/strong_user_data.sql")
    parser.add_argument("--insert-mysql", action="store_true")
    parser.add_argument("--mysql-host", type=str, default="localhost")
    parser.add_argument("--mysql-port", type=int, default=3306)
    parser.add_argument("--mysql-user", type=str, default="root")
    parser.add_argument("--mysql-password", type=str, default="root")
    parser.add_argument("--mysql-db", type=str, default="culinary_user")
    args = parser.parse_args()

    users, profiles, stats_list = await asyncio.to_thread(generate_users, args.count, args.user_id_start)

    write_sql_file(args.write_sql, users, profiles, stats_list)
    print(f"已生成 SQL 文件：{args.write_sql}")

    if args.insert_mysql:
        cfg = MysqlConfig(
            host=args.mysql_host,
            port=args.mysql_port,
            user=args.mysql_user,
            password=args.mysql_password,
            db=args.mysql_db
        )
        await asyncio.to_thread(sync_to_mysql, cfg, users, profiles, stats_list)
        print("已写入 MySQL。")

    meta = {
        "users": len(users),
        "profiles": len(profiles),
        "stats": len(stats_list)
    }
    with open(args.write_sql + ".meta.json", "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)
    return 0


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main_async()))
