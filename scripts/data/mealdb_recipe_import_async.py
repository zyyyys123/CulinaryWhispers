import argparse
import asyncio
import datetime
import json
import random
import re
from dataclasses import dataclass
from decimal import Decimal
from typing import Any, Dict, List, Optional, Sequence, Tuple

try:
    import aiohttp
except ImportError as e:
    raise SystemExit("缺少依赖 aiohttp，请先安装：pip install aiohttp") from e

try:
    import numpy as np
except ImportError as e:
    raise SystemExit("缺少依赖 numpy，请先安装：pip install numpy") from e

try:
    import pymysql
    MYSQL_AVAILABLE = True
except ImportError:
    MYSQL_AVAILABLE = False


MEALDB_RANDOM = "https://www.themealdb.com/api/json/v1/1/random.php"


@dataclass
class MysqlConfig:
    host: str
    port: int
    user: str
    password: str
    db: str
    charset: str = "utf8mb4"


IP_LOCATIONS = ["北京", "上海", "广州", "深圳", "杭州", "成都", "重庆", "武汉", "西安", "南京", "苏州", "青岛"]
DEVICE_POOL = ["iPhone 15", "Android 14 Pixel", "HarmonyOS", "Windows Web", "Mac Safari", "iPad", "Android Tablet"]

AREA_ZH: Dict[str, str] = {
    "Chinese": "中式",
    "Japanese": "日式",
    "Thai": "泰式",
    "British": "英式",
    "French": "法式",
    "Italian": "意式",
    "Spanish": "西班牙风味",
    "Greek": "希腊风味",
    "Turkish": "土耳其风味",
    "Moroccan": "摩洛哥风味",
    "Mexican": "墨西哥风味",
    "American": "美式",
    "Canadian": "加拿大风味",
    "Jamaican": "牙买加风味",
    "Filipino": "菲律宾风味",
    "Malaysian": "马来西亚风味",
    "Russian": "俄式",
    "Polish": "波兰风味",
    "Australian": "澳洲风味",
    "Saudi Arabian": "阿拉伯风味",
    "Algerian": "阿尔及利亚风味",
}

CATEGORY_ZH: Dict[str, str] = {
    "Beef": "牛肉",
    "Chicken": "鸡肉",
    "Seafood": "海鲜",
    "Lamb": "羊肉",
    "Pork": "猪肉",
    "Vegetarian": "素食",
    "Vegan": "纯素",
    "Dessert": "甜品",
    "Pasta": "意面",
    "Side": "配菜",
    "Starter": "前菜",
    "Breakfast": "早餐",
    "Miscellaneous": "综合",
    "Goat": "羊肉",
}

INGREDIENT_ZH: Dict[str, str] = {
    "Olive Oil": "橄榄油",
    "Vegetable Oil": "植物油",
    "Sesame Seed Oil": "芝麻油",
    "Butter": "黄油",
    "Garlic": "大蒜",
    "Garlic Clove": "蒜瓣",
    "Ginger": "生姜",
    "Onion": "洋葱",
    "Red Onions": "红洋葱",
    "Spring Onions": "葱",
    "Green Pepper": "青椒",
    "Red Pepper": "红椒",
    "Chilli": "辣椒",
    "Red Chilli": "红辣椒",
    "Salt": "盐",
    "Pepper": "黑胡椒",
    "Soy Sauce": "酱油",
    "Oyster Sauce": "蚝油",
    "Sugar": "白糖",
    "Flour": "面粉",
    "Egg": "鸡蛋",
    "Eggs": "鸡蛋",
    "Milk": "牛奶",
    "Tomato": "番茄",
    "Tomato Sauce": "番茄酱",
    "Lime": "青柠",
    "Lemon": "柠檬",
    "Cabbage": "卷心菜",
    "Mushrooms": "蘑菇",
    "Shiitake Mushrooms": "香菇",
    "Rice": "大米",
    "Rice Noodles": "米粉",
    "Spaghetti": "意大利面",
    "Parmesan Cheese": "帕玛森奶酪",
    "Salmon": "三文鱼",
    "Prawns": "虾",
    "Shrimp": "虾",
    "Beef": "牛肉",
    "Chicken Thighs": "鸡腿肉",
    "Carrots": "胡萝卜",
    "Potatoes": "土豆",
    "Sweet Potato": "红薯",
    "Celery": "芹菜",
    "Parsley": "欧芹",
    "Cilantro": "香菜",
}

COOKING_METHOD_ZH: List[Tuple[str, str]] = [
    ("steamed", "清蒸"),
    ("stir fry", "快炒"),
    ("stir-fry", "快炒"),
    ("fried", "香煎"),
    ("roast", "烤制"),
    ("baked", "烘烤"),
    ("grill", "炙烤"),
    ("soup", "炖煮"),
    ("salad", "凉拌"),
    ("noodles", "拌面"),
    ("cake", "蛋糕"),
    ("bread", "面包"),
]


def _ms(dt: datetime.datetime) -> str:
    return dt.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]


def _sql_escape(s: Optional[str]) -> str:
    if s is None:
        return "NULL"
    return "'" + s.replace("\\", "\\\\").replace("'", "''") + "'"


def _sql_num(n) -> str:
    if n is None:
        return "NULL"
    return str(n)


def _zipf_int(base: int, a: float, cap: int) -> int:
    v = int(np.random.zipf(a))
    return min(base * v, cap)


def _norm_int(mu: float, sigma: float, low: int, high: int) -> int:
    v = int(np.random.normal(mu, sigma))
    return int(np.clip(v, low, high))


def _split_steps(instructions: str) -> List[str]:
    if not instructions:
        return []
    raw = instructions.replace("\r\n", "\n").replace("\r", "\n")
    raw = re.sub(r"\n{2,}", "\n", raw).strip()
    parts: List[str] = []
    for line in raw.split("\n"):
        line = line.strip()
        if not line:
            continue
        line = re.sub(r"^\d+\s*[\.\)\-]\s*", "", line)
        for seg in re.split(r"[。\.]\s+", line):
            seg = seg.strip()
            if len(seg) >= 6:
                parts.append(seg)
    if not parts:
        parts = [raw[:240]] if raw else []
    parts = parts[:20]
    return parts


def _sanitize_one_line(text: str, limit: int) -> str:
    if not text:
        return ""
    t = text.replace("\r\n", " ").replace("\r", " ").replace("\n", " ")
    t = re.sub(r"\s{2,}", " ", t).strip()
    return t[:limit]


def _contains_ascii_letters(text: str) -> bool:
    return bool(re.search(r"[A-Za-z]", text or ""))


def _area_to_zh(area: Optional[str]) -> str:
    if not area:
        return "海外风味"
    area = str(area).strip()
    return AREA_ZH.get(area, "海外风味")


def _category_to_zh(category: Optional[str]) -> str:
    if not category:
        return "家常"
    category = str(category).strip()
    return CATEGORY_ZH.get(category, "家常")


def _ingredient_to_zh(name: str) -> str:
    if not name:
        return ""
    raw = str(name).strip()
    if raw in INGREDIENT_ZH:
        return INGREDIENT_ZH[raw]

    lowered = raw.lower()
    tokens = re.split(r"[\s\-/]+", lowered)
    mapping = {
        "oil": "油",
        "olive": "橄榄",
        "sesame": "芝麻",
        "butter": "黄油",
        "garlic": "大蒜",
        "ginger": "生姜",
        "onion": "洋葱",
        "pepper": "胡椒",
        "salt": "盐",
        "sugar": "糖",
        "flour": "面粉",
        "egg": "鸡蛋",
        "eggs": "鸡蛋",
        "milk": "牛奶",
        "tomato": "番茄",
        "sauce": "酱",
        "lemon": "柠檬",
        "lime": "青柠",
        "rice": "米",
        "noodles": "面",
        "beef": "牛肉",
        "chicken": "鸡肉",
        "pork": "猪肉",
        "lamb": "羊肉",
        "fish": "鱼",
        "salmon": "三文鱼",
        "shrimp": "虾",
        "prawns": "虾",
        "mushroom": "蘑菇",
        "mushrooms": "蘑菇",
        "cabbage": "卷心菜",
        "carrot": "胡萝卜",
        "carrots": "胡萝卜",
        "potato": "土豆",
        "potatoes": "土豆",
    }
    zh_parts: List[str] = []
    for t in tokens:
        if not t:
            continue
        if t in mapping:
            zh_parts.append(mapping[t])
    zh = "".join(zh_parts).strip()
    if zh and not _contains_ascii_letters(zh):
        return zh[:16]
    return ""


def _guess_method_zh(title: str, category_zh: str) -> str:
    t = (title or "").lower()
    for k, v in COOKING_METHOD_ZH:
        if k in t:
            return v
    if category_zh in ("甜品",):
        return "烘焙"
    if category_zh in ("海鲜",):
        return random.choice(["清蒸", "香煎", "红烧"])
    if category_zh in ("牛肉", "羊肉"):
        return random.choice(["炖煮", "红烧", "香煎"])
    if category_zh in ("鸡肉", "猪肉"):
        return random.choice(["炖煮", "快炒", "烤制"])
    if category_zh in ("素食", "纯素"):
        return random.choice(["清炒", "凉拌", "炖煮"])
    return random.choice(["家常", "快手", "下饭"])


def _device_to_zh(device: str) -> str:
    d = (device or "").strip()
    mapping = {
        "Windows Web": "Windows 网页端",
        "Mac Safari": "Mac 浏览器",
        "Android Tablet": "安卓平板",
        "Android 14 Pixel": "安卓手机",
        "iPhone 15": "iPhone",
        "iPad": "iPad",
        "HarmonyOS": "HarmonyOS"
    }
    return mapping.get(d, "移动端")


def _build_cn_recipe(meal: Dict[str, Any]) -> Tuple[str, str, List[str], List[str]]:
    area_zh = _area_to_zh(meal.get("strArea"))
    category_zh = _category_to_zh(meal.get("strCategory"))
    ingredients = _extract_ingredients(meal)
    ing_zh_list = []
    for ing, _ in ingredients:
        zh = _ingredient_to_zh(ing)
        if zh:
            ing_zh_list.append(zh)
    ing_zh_list = list(dict.fromkeys(ing_zh_list))[:6]
    main_ing = ing_zh_list[0] if ing_zh_list else category_zh

    title_raw = str(meal.get("strMeal") or "")
    method_zh = _guess_method_zh(title_raw, category_zh)
    title_zh = f"{area_zh}{main_ing}{method_zh}"
    title_zh = title_zh.replace("风味风味", "风味")[:128]

    extra = "、".join(ing_zh_list[1:4])
    if extra:
        desc = f"{area_zh}{category_zh}风味，主料以{main_ing}为主，搭配{extra}，口感层次丰富。"
    else:
        desc = f"{area_zh}{category_zh}风味，主料以{main_ing}为主，适合日常家常烹饪。"
    desc = _sanitize_one_line(desc, 800)

    steps: List[str] = []
    steps.append(f"准备食材：{('、'.join(ing_zh_list) if ing_zh_list else '常用食材')}。")
    steps.append(f"{main_ing}处理干净并切配备用，辅料按需切丝/切丁。")
    steps.append("调制基础调味汁：酱油、盐、少许糖（可按口味调整）。")
    steps.append(f"热锅少油，下主料{main_ing}{'快炒' if method_zh in ('快炒', '清炒') else '煎至'}熟香。")
    steps.append("加入辅料与调味汁翻拌均匀，小火收汁入味。")
    steps.append("装盘点缀，趁热享用。")

    tags = [area_zh, category_zh, main_ing, method_zh, random.choice(["家常", "快手", "下饭", "清爽", "解馋"])]
    tags = [t for t in tags if t and not _contains_ascii_letters(t)]
    tags = list(dict.fromkeys(tags))[:12]

    return title_zh, desc, steps, tags


def _extract_tags(meal: Dict[str, Any]) -> List[str]:
    tags: List[str] = []
    if meal.get("strTags"):
        tags.extend([t.strip() for t in str(meal["strTags"]).split(",") if t and t.strip()])
    area = meal.get("strArea")
    cat = meal.get("strCategory")
    if area:
        tags.append(str(area).strip())
    if cat:
        tags.append(str(cat).strip())
    ingredients = _extract_ingredients(meal)
    for ing, _ in ingredients[:6]:
        if ing:
            tags.append(ing)
    uniq: List[str] = []
    seen = set()
    for t in tags:
        if not t:
            continue
        t = t[:32]
        if t not in seen:
            seen.add(t)
            uniq.append(t)
    return uniq[:12]


def _extract_ingredients(meal: Dict[str, Any]) -> List[Tuple[str, str]]:
    pairs: List[Tuple[str, str]] = []
    for i in range(1, 21):
        ing = meal.get(f"strIngredient{i}")
        mea = meal.get(f"strMeasure{i}")
        ing = (str(ing).strip() if ing else "")
        mea = (str(mea).strip() if mea else "")
        if ing:
            pairs.append((ing, mea))
    return pairs


def _category_id(meal_category: Optional[str]) -> int:
    if not meal_category:
        return 1
    c = str(meal_category).strip().lower()
    if c in ("dessert",):
        return 2
    if c in ("cocktail", "shake", "drink"):
        return 4
    if c in ("starter",):
        return 3
    return 1


def _difficulty(steps_count: int) -> int:
    if steps_count <= 4:
        return 1
    if steps_count <= 7:
        return 2
    if steps_count <= 11:
        return 3
    if steps_count <= 15:
        return 4
    return 5


async def _fetch_random_meal(session: aiohttp.ClientSession) -> Optional[Dict[str, Any]]:
    async with session.get(MEALDB_RANDOM, timeout=aiohttp.ClientTimeout(total=15)) as resp:
        if resp.status != 200:
            return None
        data = await resp.json()
        meals = data.get("meals") if isinstance(data, dict) else None
        if not meals:
            return None
        meal = meals[0]
        if not isinstance(meal, dict):
            return None
        return meal


async def fetch_meals(count: int, concurrency: int) -> List[Dict[str, Any]]:
    connector = aiohttp.TCPConnector(limit=concurrency)
    async with aiohttp.ClientSession(connector=connector) as session:
        results: List[Dict[str, Any]] = []
        seen_ids = set()

        sem = asyncio.Semaphore(concurrency)

        async def one() -> None:
            nonlocal results
            async with sem:
                meal = await _fetch_random_meal(session)
                if not meal:
                    return
                mid = meal.get("idMeal")
                if not mid:
                    return
                if mid in seen_ids:
                    return
                seen_ids.add(mid)
                results.append(meal)

        tasks = []
        while len(results) < count:
            tasks = [asyncio.create_task(one()) for _ in range(max(concurrency, 8))]
            await asyncio.gather(*tasks, return_exceptions=True)
            if len(results) >= count:
                break
        return results[:count]


def build_sql(meals: List[Dict[str, Any]], recipe_id_start: int, author_id_pool: Sequence[int]) -> str:
    now = datetime.datetime.now()
    lines: List[str] = []
    lines.append("USE culinary_user;")
    lines.append("")

    recipe_rows: List[Dict[str, Any]] = []
    step_rows: List[Dict[str, Any]] = []
    stats_rows: List[Dict[str, Any]] = []
    tag_names: set = set()
    relations: List[Tuple[int, str]] = []

    for idx, meal in enumerate(meals):
        recipe_id = recipe_id_start + idx
        author_id = int(random.choice(list(author_id_pool))) if author_id_pool else 1
        title, desc, steps, tags = _build_cn_recipe(meal)
        cover_url = str(meal.get("strMealThumb") or "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1800&q=80")[:512]
        video_url = str(meal.get("strYoutube") or "")[:512]
        cat_id = _category_id(meal.get("strCategory"))
        tags_json = json.dumps(tags, ensure_ascii=False)

        difficulty = _difficulty(len(steps))
        time_cost = int(np.clip(len(steps) * np.random.normal(12, 5), 5, 240))
        calories = _norm_int(520, 220, 60, 1600)
        protein = Decimal(str(round(float(np.clip(np.random.normal(25, 15), 0, 180)), 2)))
        fat = Decimal(str(round(float(np.clip(np.random.normal(18, 12), 0, 140)), 2)))
        carbs = Decimal(str(round(float(np.clip(np.random.normal(55, 30), 0, 260)), 2)))
        score = Decimal(str(round(float(np.clip(np.random.normal(4.2, 0.45), 0, 5)), 1)))

        view_count = _zipf_int(base=120, a=1.55, cap=1_800_000)
        like_count = int(view_count * np.clip(np.random.normal(0.06, 0.03), 0.005, 0.25))
        collect_count = int(view_count * np.clip(np.random.normal(0.028, 0.018), 0.003, 0.18))
        comment_count = int(view_count * np.clip(np.random.normal(0.008, 0.006), 0.0005, 0.08))
        share_count = int(view_count * np.clip(np.random.normal(0.006, 0.005), 0.0002, 0.08))
        try_count = int(view_count * np.clip(np.random.normal(0.002, 0.002), 0.0, 0.05))

        gmt_create = now - datetime.timedelta(days=random.randint(0, 320), hours=random.randint(0, 23))
        gmt_modified = gmt_create + datetime.timedelta(hours=random.randint(0, 240))

        recipe_rows.append({
            "id": recipe_id,
            "author_id": author_id,
            "title": title,
            "cover_url": cover_url,
            "video_url": video_url,
            "description": desc,
            "category_id": cat_id,
            "cuisine_id": None,
            "difficulty": difficulty,
            "time_cost": time_cost,
            "calories": calories,
            "protein": str(protein),
            "fat": str(fat),
            "carbs": str(carbs),
            "score": str(score),
            "view_count": view_count,
            "like_count": like_count,
            "collect_count": collect_count,
            "comment_count": comment_count,
            "share_count": share_count,
            "try_count": try_count,
            "status": 2,
            "tags": tags_json,
            "tips": f"建议搭配：{random.choice(['清爽沙拉', '热汤', '气泡水', '水果', '咖啡'])}",
            "is_exclusive": 1 if random.random() < 0.08 else 0,
            "is_paid": 1 if random.random() < 0.06 else 0,
            "price": str(Decimal(str(round(float(np.clip(np.random.normal(6, 5), 0, 49.9)), 2)))),
            "publish_time": _ms(gmt_create),
            "ip_location": random.choice(IP_LOCATIONS),
            "device_info": _device_to_zh(random.choice(DEVICE_POOL)),
            "gmt_create": _ms(gmt_create),
            "gmt_modified": _ms(gmt_modified),
            "is_deleted": 0,
            "version": 1
        })

        for s_idx, s in enumerate(steps, start=1):
            step_rows.append({
                "id": recipe_id * 100 + s_idx,
                "recipe_id": recipe_id,
                "step_no": s_idx,
                "desc": _sanitize_one_line(s, 1200),
                "img_url": cover_url if s_idx == 1 else None,
                "video_url": video_url if s_idx == 1 else None,
                "time_cost": int(np.clip(np.random.normal(8, 4), 1, 60)),
                "is_key_step": 1 if (s_idx == 1 or (s_idx % 4 == 0)) else 0,
                "voice_url": None,
                "gmt_create": _ms(gmt_create)
            })

        stats_rows.append({
            "recipe_id": recipe_id,
            "view_count": view_count,
            "like_count": like_count,
            "collect_count": collect_count,
            "comment_count": comment_count,
            "share_count": share_count,
            "try_count": try_count,
            "score": str(score),
            "gmt_modified": _ms(gmt_modified)
        })

        for t in tags:
            tag_names.add(t)
            relations.append((recipe_id, t))

    for r in recipe_rows:
        cols = [
            "id", "author_id", "title", "cover_url", "video_url", "description", "category_id", "cuisine_id",
            "difficulty", "time_cost", "calories", "protein", "fat", "carbs",
            "score", "view_count", "like_count", "collect_count", "comment_count", "share_count", "try_count",
            "status", "tags", "tips", "is_exclusive", "is_paid", "price", "publish_time",
            "ip_location", "device_info", "gmt_create", "gmt_modified", "is_deleted", "version"
        ]
        values = [
            _sql_num(r["id"]),
            _sql_num(r["author_id"]),
            _sql_escape(r["title"]),
            _sql_escape(r["cover_url"]),
            _sql_escape(r["video_url"]),
            _sql_escape(r["description"]),
            _sql_num(r["category_id"]),
            _sql_num(r["cuisine_id"]),
            _sql_num(r["difficulty"]),
            _sql_num(r["time_cost"]),
            _sql_num(r["calories"]),
            _sql_num(r["protein"]),
            _sql_num(r["fat"]),
            _sql_num(r["carbs"]),
            _sql_num(r["score"]),
            _sql_num(r["view_count"]),
            _sql_num(r["like_count"]),
            _sql_num(r["collect_count"]),
            _sql_num(r["comment_count"]),
            _sql_num(r["share_count"]),
            _sql_num(r["try_count"]),
            _sql_num(r["status"]),
            _sql_escape(r["tags"]),
            _sql_escape(r["tips"]),
            _sql_num(r["is_exclusive"]),
            _sql_num(r["is_paid"]),
            _sql_num(r["price"]),
            _sql_escape(r["publish_time"]),
            _sql_escape(r["ip_location"]),
            _sql_escape(r["device_info"]),
            _sql_escape(r["gmt_create"]),
            _sql_escape(r["gmt_modified"]),
            _sql_num(r["is_deleted"]),
            _sql_num(r["version"]),
        ]
        lines.append(f"REPLACE INTO t_rcp_info ({', '.join(cols)}) VALUES ({', '.join(values)});")

    lines.append("")

    for s in step_rows:
        cols = ["id", "recipe_id", "step_no", "`desc`", "img_url", "video_url", "time_cost", "is_key_step", "voice_url", "gmt_create"]
        values = [
            _sql_num(s["id"]),
            _sql_num(s["recipe_id"]),
            _sql_num(s["step_no"]),
            _sql_escape(s["desc"]),
            _sql_escape(s["img_url"]),
            _sql_escape(s["video_url"]),
            _sql_num(s["time_cost"]),
            _sql_num(s["is_key_step"]),
            _sql_escape(s["voice_url"]),
            _sql_escape(s["gmt_create"]),
        ]
        lines.append(f"REPLACE INTO t_rcp_step ({', '.join(cols)}) VALUES ({', '.join(values)});")

    lines.append("")

    for st in stats_rows:
        cols = ["recipe_id", "view_count", "like_count", "collect_count", "comment_count", "share_count", "try_count", "score", "gmt_modified"]
        values = [
            _sql_num(st["recipe_id"]),
            _sql_num(st["view_count"]),
            _sql_num(st["like_count"]),
            _sql_num(st["collect_count"]),
            _sql_num(st["comment_count"]),
            _sql_num(st["share_count"]),
            _sql_num(st["try_count"]),
            _sql_num(st["score"]),
            _sql_escape(st["gmt_modified"]),
        ]
        lines.append(f"REPLACE INTO t_rcp_stats ({', '.join(cols)}) VALUES ({', '.join(values)});")

    lines.append("")

    for name in sorted(tag_names):
        lines.append(f"INSERT IGNORE INTO t_rcp_tag (name, type, use_count) VALUES ({_sql_escape(name)}, 1, 0);")

    lines.append("")

    for recipe_id, tag in relations:
        lines.append(
            "INSERT IGNORE INTO t_rcp_tag_relation (recipe_id, tag_id) "
            f"SELECT {recipe_id}, id FROM t_rcp_tag WHERE name={_sql_escape(tag)};"
        )

    lines.append("")
    return "\n".join(lines) + "\n"


def _execute_sql_batch(conn, sql: str, data: List[Tuple], batch_size: int = 200) -> None:
    with conn.cursor() as cursor:
        for i in range(0, len(data), batch_size):
            cursor.executemany(sql, data[i:i + batch_size])
    conn.commit()


def insert_mysql(cfg: MysqlConfig, meals: List[Dict[str, Any]], recipe_id_start: int, author_id_pool: Sequence[int]) -> None:
    if not MYSQL_AVAILABLE:
        raise SystemExit("缺少依赖 pymysql，无法直连 MySQL。请先安装：pip install pymysql")
    sql_text = build_sql(meals, recipe_id_start, author_id_pool)
    conn = pymysql.connect(
        host=cfg.host,
        port=cfg.port,
        user=cfg.user,
        password=cfg.password,
        db=cfg.db,
        charset=cfg.charset,
        autocommit=False
    )
    try:
        with conn.cursor() as cursor:
            for line in sql_text.splitlines():
                stmt = line.strip()
                if not stmt:
                    continue
                if stmt.upper().startswith("USE "):
                    continue
                if not stmt.endswith(";"):
                    continue
                cursor.execute(stmt[:-1])
        conn.commit()
    finally:
        conn.close()


async def main_async() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--count", type=int, default=60)
    parser.add_argument("--concurrency", type=int, default=10)
    parser.add_argument("--recipe-id-start", type=int, default=200000)
    parser.add_argument("--author-ids", type=str, default="1")
    parser.add_argument("--write-sql", type=str, default="sql/mealdb_recipes.sql")
    parser.add_argument("--insert-mysql", action="store_true")
    parser.add_argument("--mysql-host", type=str, default="localhost")
    parser.add_argument("--mysql-port", type=int, default=3306)
    parser.add_argument("--mysql-user", type=str, default="root")
    parser.add_argument("--mysql-password", type=str, default="root")
    parser.add_argument("--mysql-db", type=str, default="culinary_user")
    args = parser.parse_args()

    author_ids = [int(x) for x in re.split(r"[,\s]+", args.author_ids.strip()) if x.strip()]
    meals = await fetch_meals(args.count, args.concurrency)

    sql_text = await asyncio.to_thread(build_sql, meals, args.recipe_id_start, author_ids)
    with open(args.write_sql, "w", encoding="utf-8") as f:
        f.write(sql_text)
    print(f"已生成 SQL 文件：{args.write_sql}")

    if args.insert_mysql:
        cfg = MysqlConfig(
            host=args.mysql_host,
            port=args.mysql_port,
            user=args.mysql_user,
            password=args.mysql_password,
            db=args.mysql_db
        )
        await asyncio.to_thread(insert_mysql, cfg, meals, args.recipe_id_start, author_ids)
        print("已写入 MySQL。")

    meta = {
        "meals": len(meals),
        "recipeIdStart": args.recipe_id_start
    }
    with open(args.write_sql + ".meta.json", "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)
    return 0


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main_async()))
