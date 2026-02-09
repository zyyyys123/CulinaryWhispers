import os
import random
import datetime
import uuid
import json
from decimal import Decimal, getcontext
import faker
import numpy as np

# 尝试导入 elasticsearch 库
# 如果环境没有安装该库，将跳过 ES 同步步骤
try:
    from elasticsearch import Elasticsearch, helpers
    ES_AVAILABLE = True
except ImportError:
    ES_AVAILABLE = False
    print("警告: 未找到 'elasticsearch' 模块，将跳过 ES 数据同步。")

# 初始化 Faker 生成器，支持中、英、日三种语言环境
fake = faker.Faker(['zh_CN', 'en_US', 'ja_JP'])

# 输出 SQL 文件路径
MYSQL_FILE = "mysql_chaos_data.sql"
DORIS_FILE = "doris_chaos_data.sql"

# 常量定义
USER_COUNT = 100        # 生成用户数量
TRANS_COUNT = 1000      # 生成交易记录数量
START_DATE = datetime.date(2025, 1, 1)  # 数据起始日期
END_DATE = datetime.date(2026, 12, 31)  # 数据结束日期
ES_HOST = "http://localhost:9200"       # ES 服务地址

# 混沌工程配置 (Chaos Engineering)
CHAOS_PROBABILITY = 0.15  # 脏数据生成的概率 (15%)
SPECIAL_CHARS = [
    "Robert'); DROP TABLE Students;--",  # SQL 注入测试
    "🌟✨🔥",                             # Emoji 字符
    "   ",                               # 纯空格
    "\\u0000",                           # 空字节
    "admin",                             # 敏感词
    "<script>alert('XSS')</script>",     # XSS 攻击测试
    "null",                              # 字符串 "null"
    "undefined",                         # 字符串 "undefined"
    "1 OR 1=1",                          # 逻辑注入
    "Thinking... \n Multi-line",         # 多行文本
    "😊😂🤣❤️😍",                        # 更多 Emoji
    "very_long_string_" * 10             # 超长字符串
]

def get_chaos_string(base_str):
    """
    根据设定的概率，将正常字符串替换为脏数据或特殊字符，
    用于测试系统的健壮性和对异常数据的处理能力。
    """
    if random.random() < CHAOS_PROBABILITY:
        return random.choice(SPECIAL_CHARS)
    return base_str

def get_random_date():
    """
    生成 START_DATE 和 END_DATE 之间的随机日期。
    """
    days_range = (END_DATE - START_DATE).days
    random_days = random.randint(0, days_range)
    return START_DATE + datetime.timedelta(days=random_days)

def get_pareto_amount():
    """
    使用帕累托分布生成交易金额。
    符合二八定律：20% 的交易占据了 80% 的金额大小，模拟真实的消费场景。
    alpha=1.16 大约对应 80/20 分布。
    """
    amount = (np.random.pareto(a=1.16) + 1) * 10
    return round(Decimal(amount), 2)

class DataFactory:
    """
    数据工厂类，负责生成用户、食谱、交易等模拟数据，
    并将其导出为 MySQL SQL、Doris SQL 以及同步到 Elasticsearch。
    """
    def __init__(self):
        self.users = []         # 用户列表
        self.transactions = []  # 交易记录列表
        self.recipes = []       # 食谱列表
        self.categories = []    # 分类列表
        self.stats = []         # 统计数据列表
        self.daily_stats = {}   # 每日统计聚合 (date, merchant_id, biz_type) -> {amount, count}

    def generate_categories(self):
        """
        生成食谱分类数据。
        构建简单的二级分类树结构 (例如: 家常菜 -> 川菜)。
        """
        print("正在生成分类数据 (Categories)...")
        roots = ['家常菜', '烘焙', '西餐', '饮品']
        subs = {
            '家常菜': ['川菜', '粤菜', '湘菜', '汤羹'],
            '烘焙': ['蛋糕', '面包', '饼干'],
            '西餐': ['牛排', '意面', '沙拉'],
            '饮品': ['果汁', '茶饮', '咖啡']
        }
        
        id_counter = 1
        for r in roots:
            root_id = id_counter
            # 添加一级分类
            self.categories.append({
                'id': root_id, 'name': r, 'parent_id': 0, 'level': 1, 'sort': id_counter
            })
            id_counter += 1
            for s in subs.get(r, []):
                # 添加二级分类
                self.categories.append({
                    'id': id_counter, 'name': s, 'parent_id': root_id, 'level': 2, 'sort': id_counter
                })
                id_counter += 1

    def generate_recipes(self):
        """
        生成食谱数据及其关联的统计数据。
        依赖于用户数据（作者）和分类数据。
        """
        print("正在生成食谱数据 (Recipes)...")
        if not self.users:
            return

        for i in range(1, 501): # 生成 500 个食谱
            user = random.choice(self.users)
            cat = random.choice(self.categories)
            
            # 生成过去一年内的随机创建时间
            gmt_create = fake.date_time_between(start_date='-1y', end_date='now')
            
            rcp = {
                'id': i + 10000, # ID 从 10000 开始
                'author_id': user['id'],
                'title': get_chaos_string(fake.sentence(nb_words=4)), # 标题可能包含脏数据
                'cover_url': fake.image_url(),
                'video_url': "",
                'description': fake.text(max_nb_chars=100),
                'category_id': cat['id'],
                'cuisine_id': 0,
                'difficulty': random.randint(1, 5), # 难度 1-5 星
                'time_cost': random.choice([10, 30, 60, 120]), # 耗时
                'calories': random.randint(100, 1000), # 卡路里
                'score': round(random.uniform(3.0, 5.0), 1), # 评分 3.0-5.0
                'status': 2, # 状态: 2-已发布
                'gmt_create': gmt_create.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
                'gmt_modified': gmt_create.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
                'is_deleted': 0,
                'version': 1
            }
            self.recipes.append(rcp)
            
            # 生成关联的统计数据 (点赞、收藏等)
            stat = {
                'recipe_id': rcp['id'],
                'view_count': random.randint(100, 10000),
                'like_count': random.randint(0, 1000),
                'collect_count': random.randint(0, 500),
                'comment_count': random.randint(0, 100),
                'share_count': random.randint(0, 50),
                'try_count': random.randint(0, 20),
                'score': rcp['score'],
                'gmt_modified': rcp['gmt_modified']
            }
            self.stats.append(stat)

    def generate_users(self):
        """
        生成用户数据。
        包含基础信息（用户名、手机号等）和 画像维度信息（等级、消费总额等）。
        """
        print(f"正在生成 {USER_COUNT} 个用户数据...")
        for i in range(1, USER_COUNT + 1):
            gmt_create = fake.date_time_between(start_date='-2y', end_date='now')
            last_login = fake.date_time_between(start_date=gmt_create, end_date='now')
            
            user = {
                'id': i,
                'username': get_chaos_string(fake.user_name() + str(i)), # 确保基础用户名唯一
                'mobile': fake.phone_number(),
                'email': get_chaos_string(fake.email()),
                'password_hash': fake.sha256(),
                'salt': fake.uuid4()[:32],
                'nickname': get_chaos_string(fake.name()),
                'avatar_url': get_chaos_string(fake.image_url()),
                'status': random.choices([1, 2, 3], weights=[80, 15, 5])[0], # 状态权重: 80%正常, 15%冻结, 5%注销
                'register_source': random.choice(['APP', 'WEB', 'WX', 'MINIPROGRAM', 'UNKNOWN']),
                'register_ip': fake.ipv4(),
                'last_login_time': last_login.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
                'last_login_ip': fake.ipv4(),
                'gmt_create': gmt_create.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
                'gmt_modified': last_login.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
                'is_deleted': 0,
                'version': 1
            }
            # 用户画像字段 (用于 Doris dim_user_profile 表)
            user['level'] = random.randint(1, 100)
            user['reg_time'] = user['gmt_create']
            user['total_spend'] = round(random.uniform(0, 10000), 2)
            
            self.users.append(user)

    def generate_transactions(self):
        """
        生成交易流水数据。
        模拟充值、消费、佣金等业务场景，并聚合每日数据用于 OLAP 分析。
        """
        print(f"正在生成 {TRANS_COUNT} 条交易记录...")
        
        # 业务类型与资金流向的映射关系
        # biz_type: 1-充值(收入), 2-消费(支出), 5-佣金(收入)
        # flow_type: 1-收入, 2-支出
        BIZ_FLOW_MAP = {
            1: 1, # 充值 -> 收入
            2: 2, # 消费 -> 支出
            5: 1  # 佣金 -> 收入
        }

        for i in range(1, TRANS_COUNT + 1):
            user = random.choice(self.users)
            amount = get_pareto_amount() # 使用帕累托分布生成金额
            biz_type = random.choice([1, 2, 5])
            flow_type = BIZ_FLOW_MAP.get(biz_type, 1)
            
            # 确保交易时间在用户注册之后
            user_reg_time = datetime.datetime.strptime(user['gmt_create'], "%Y-%m-%d %H:%M:%S")
            if user_reg_time > datetime.datetime.now():
                user_reg_time = datetime.datetime.now() - datetime.timedelta(days=1)
                
            gmt_create_dt = fake.date_time_between(start_date=user_reg_time, end_date='now')
            gmt_create = gmt_create_dt.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
            
            # 提取日期用于聚合统计
            stat_date = gmt_create_dt.strftime("%Y-%m-%d")
            
            trans = {
                'id': i,
                'transaction_no': uuid.uuid4().hex,
                'account_id': user['id'], 
                'biz_order_no': f"ORDER_{uuid.uuid4().hex[:12]}",
                'out_trade_no': f"PAY_{uuid.uuid4().hex[:16]}",
                'user_id': user['id'],
                'account_type': random.randint(1, 4), # 账户类型: 余额、红包等
                'flow_type': flow_type,
                'biz_type': biz_type,
                'amount': amount,
                'balance_after': round(Decimal(random.uniform(float(amount), 50000)), 2), # 模拟变动后余额
                'status': random.choices([0, 1, 2], weights=[10, 85, 5])[0], # 0-处理中, 1-成功, 2-失败
                'remark': get_chaos_string(fake.sentence()),
                'gmt_create': gmt_create,
                'gmt_modified': gmt_create
            }
            self.transactions.append(trans)

            # 聚合每日结算数据 (用于 Doris)
            # 逻辑: 仅统计成功的交易 (这里为了演示方便，统计了所有状态为 1 的交易)
            if trans['status'] == 1:
                key = (stat_date, user['id'], biz_type) 
                if key not in self.daily_stats:
                    self.daily_stats[key] = {'total_amount': Decimal(0), 'trans_count': 0}
                self.daily_stats[key]['total_amount'] += amount
                self.daily_stats[key]['trans_count'] += 1

    def write_mysql_sql(self):
        """
        将生成的数据写入 MySQL 初始化脚本 (mysql_chaos_data.sql)。
        """
        print(f"正在写入 MySQL 脚本: {MYSQL_FILE} ...")
        with open(MYSQL_FILE, 'w', encoding='utf-8') as f:
            f.write("USE culinary_user;\n\n")
            
            # 写入用户表 t_usr_base
            f.write("-- t_usr_base (用户基础表)\n")
            for u in self.users:
                sql = f"INSERT INTO t_usr_base (id, username, mobile, email, password_hash, salt, nickname, avatar_url, status, register_source, register_ip, last_login_time, last_login_ip, gmt_create, gmt_modified, is_deleted, version) VALUES ({u['id']}, '{u['username']}', '{u['mobile']}', '{u['email']}', '{u['password_hash']}', '{u['salt']}', '{u['nickname']}', '{u['avatar_url']}', {u['status']}, '{u['register_source']}', '{u['register_ip']}', '{u['last_login_time']}', '{u['last_login_ip']}', '{u['gmt_create']}', '{u['gmt_modified']}', {u['is_deleted']}, {u['version']});\n"
                f.write(sql)
            
            # 写入分类表 t_rcp_category
            f.write("\n-- t_rcp_category (食谱分类表)\n")
            for c in self.categories:
                sql = f"INSERT INTO t_rcp_category (id, name, parent_id, level, sort, is_visible) VALUES ({c['id']}, '{c['name']}', {c['parent_id']}, {c['level']}, {c['sort']}, 1);\n"
                f.write(sql)

            # 写入食谱表 t_rcp_info
            f.write("\n-- t_rcp_info (食谱信息表)\n")
            for r in self.recipes:
                # 查找对应的统计数据
                s = next((x for x in self.stats if x['recipe_id'] == r['id']), None)
                v_count = s['view_count'] if s else 0
                
                sql = f"INSERT INTO t_rcp_info (id, author_id, title, cover_url, description, category_id, cuisine_id, difficulty, time_cost, calories, score, view_count, like_count, collect_count, comment_count, share_count, try_count, status, gmt_create, gmt_modified, is_deleted, version) VALUES ({r['id']}, {r['author_id']}, '{r['title']}', '{r['cover_url']}', '{r['description']}', {r['category_id']}, {r['cuisine_id']}, {r['difficulty']}, {r['time_cost']}, {r['calories']}, {r['score']}, {v_count}, {s['like_count']}, {s['collect_count']}, {s['comment_count']}, {s['share_count']}, {s['try_count']}, {r['status']}, '{r['gmt_create']}', '{r['gmt_modified']}', {r['is_deleted']}, {r['version']});\n"
                f.write(sql)

            # 写入食谱统计表 t_rcp_stats
            f.write("\n-- t_rcp_stats (食谱统计表)\n")
            for s in self.stats:
                sql = f"INSERT INTO t_rcp_stats (recipe_id, view_count, like_count, collect_count, comment_count, share_count, try_count, score, gmt_modified) VALUES ({s['recipe_id']}, {s['view_count']}, {s['like_count']}, {s['collect_count']}, {s['comment_count']}, {s['share_count']}, {s['try_count']}, {s['score']}, '{s['gmt_modified']}');\n"
                f.write(sql)

            # 写入交易流水表 t_fin_transaction_flow
            f.write("\n-- t_fin_transaction_flow (交易流水表)\n")
            for t in self.transactions:
                sql = f"INSERT INTO t_fin_transaction_flow (id, transaction_no, account_id, biz_order_no, out_trade_no, user_id, account_type, flow_type, biz_type, amount, balance_after, status, remark, gmt_create, gmt_modified) VALUES ({t['id']}, '{t['transaction_no']}', {t['account_id']}, '{t['biz_order_no']}', '{t['out_trade_no']}', {t['user_id']}, {t['account_type']}, {t['flow_type']}, {t['biz_type']}, {t['amount']}, {t['balance_after']}, {t['status']}, '{t['remark']}', '{t['gmt_create']}', '{t['gmt_modified']}');\n"
                f.write(sql)

    def write_doris_sql(self):
        """
        将生成的数据写入 Doris 初始化脚本 (doris_chaos_data.sql)。
        主要包含维度表和聚合表的数据。
        """
        print(f"正在写入 Doris 脚本: {DORIS_FILE} ...")
        with open(DORIS_FILE, 'w', encoding='utf-8') as f:
            f.write("USE culinary_dw;\n\n")
            
            # 写入用户维度表 dim_user_profile
            f.write("-- dim_user_profile (用户画像维度表)\n")
            for u in self.users:
                sql = f"INSERT INTO dim_user_profile (user_id, nickname, level, reg_time, last_login_time, total_spend) VALUES ({u['id']}, '{u['nickname']}', {u['level']}, '{u['reg_time']}', '{u['last_login_time']}', {u['total_spend']});\n"
                f.write(sql)

            # 写入每日结算聚合表 dws_fin_daily_settlement
            f.write("\n-- dws_fin_daily_settlement (每日资金结算聚合表)\n")
            for (date, merchant_id, biz_type), stats in self.daily_stats.items():
                sql = f"INSERT INTO dws_fin_daily_settlement (stat_date, merchant_id, biz_type, total_amount, trans_count) VALUES ('{date}', {merchant_id}, {biz_type}, {stats['total_amount']}, {stats['trans_count']});\n"
                f.write(sql)

            # 写入食谱每日统计聚合表 dws_recipe_daily_stats
            f.write("\n-- dws_recipe_daily_stats (食谱每日数据聚合表)\n")
            # 模拟逻辑：为每个食谱生成一条“当天”的统计数据
            for r in self.recipes:
                s = next((x for x in self.stats if x['recipe_id'] == r['id']), None)
                if s:
                    # 使用 gmt_modified 的日期部分作为统计日期
                    stat_date = s['gmt_modified'].split(' ')[0]
                    # 随机生成当天的增量数据 (略小于总数)
                    daily_view = random.randint(0, s['view_count'])
                    daily_like = random.randint(0, s['like_count'])
                    daily_comment = random.randint(0, s['comment_count'])
                    daily_share = random.randint(0, s['share_count'])
                    
                    sql = f"INSERT INTO dws_recipe_daily_stats (stat_date, recipe_id, category_id, view_count, like_count, comment_count, share_count) VALUES ('{stat_date}', {r['id']}, {r['category_id']}, {daily_view}, {daily_like}, {daily_comment}, {daily_share});\n"
                    f.write(sql)

    def sync_to_es(self):
        """
        将食谱数据同步到 Elasticsearch。
        实现 'MySQL -> ES' 的数据同步逻辑，用于搜索功能的测试。
        """
        if not ES_AVAILABLE:
            print("跳过 ES 同步 (未检测到 elasticsearch 模块)")
            return
            
        print("正在同步食谱数据到 Elasticsearch...")
        es = Elasticsearch(ES_HOST)
        
        # 检查 ES 是否可用
        try:
            if not es.ping():
                print(f"无法连接到 ES ({ES_HOST})，跳过同步。")
                return
        except Exception as e:
             print(f"连接 ES 出错: {e}。跳过同步。")
             return

        index_name = "recipe_index"
        
        # 准备批量操作数据
        actions = []
        for r in self.recipes:
            # 模拟生成 3 个随机标签
            tags = [fake.word() for _ in range(3)] 
            
            doc = {
                "_index": index_name,
                "_id": r['id'],
                "_source": {
                    "id": r['id'],
                    "title": r['title'],
                    "description": r['description'],
                    "authorId": r['author_id'],
                    "tags": tags,
                    "status": r['status'],
                    "createTime": r['gmt_create']
                }
            }
            actions.append(doc)
            
        try:
            # 执行批量插入
            success, failed = helpers.bulk(es, actions, stats_only=True)
            print(f"ES 同步结果: 成功索引 {success} 条文档，失败 {failed} 条。")
        except Exception as e:
            print(f"ES 批量操作错误: {e}")

if __name__ == "__main__":
    # 实例化工厂并执行生成流程
    factory = DataFactory()
    factory.generate_users()
    factory.generate_categories()
    factory.generate_recipes()
    factory.generate_transactions()
    
    # 导出 SQL 文件
    factory.write_mysql_sql()
    factory.write_doris_sql()
    
    # 同步到 ES
    factory.sync_to_es()
    
    print(f"完成! 已生成文件: {MYSQL_FILE} 和 {DORIS_FILE}")
