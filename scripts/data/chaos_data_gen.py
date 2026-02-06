import os
import random
import datetime
import uuid
from decimal import Decimal, getcontext
import faker
import numpy as np

# Initialize Faker
fake = faker.Faker(['zh_CN', 'en_US', 'ja_JP'])

# Output files
MYSQL_FILE = "mysql_chaos_data.sql"
DORIS_FILE = "doris_chaos_data.sql"

# Constants
USER_COUNT = 100
TRANS_COUNT = 1000
START_DATE = datetime.date(2025, 1, 1)
END_DATE = datetime.date(2026, 12, 31)

# Chaos Configuration
CHAOS_PROBABILITY = 0.15  # Increased chaos probability
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
    if random.random() < CHAOS_PROBABILITY:
        return random.choice(SPECIAL_CHARS)
    return base_str

def get_random_date():
    days_range = (END_DATE - START_DATE).days
    random_days = random.randint(0, days_range)
    return START_DATE + datetime.timedelta(days=random_days)

def get_pareto_amount():
    # 80/20 rule: 20% of transactions have 80% of volume
    # alpha=1.16 gives 80/20
    amount = (np.random.pareto(a=1.16) + 1) * 10
    return round(Decimal(amount), 2)

class DataFactory:
    def __init__(self):
        self.users = []
        self.transactions = []
        self.recipes = [] # list of dict
        self.categories = [] # list of dict
        self.stats = [] # list of dict
        self.daily_stats = {} # (date, merchant_id, biz_type) -> {amount, count}

    def generate_categories(self):
        print("Generating Categories...")
        # Simple 2-level hierarchy
        roots = ['Home Cooking', 'Baking', 'Western', 'Beverage']
        subs = {
            'Home Cooking': ['Sichuan', 'Cantonese', 'Spicy', 'Soup'],
            'Baking': ['Cake', 'Bread', 'Cookie'],
            'Western': ['Steak', 'Pasta', 'Salad'],
            'Beverage': ['Juice', 'Tea', 'Coffee']
        }
        
        id_counter = 1
        for r in roots:
            root_id = id_counter
            self.categories.append({
                'id': root_id, 'name': r, 'parent_id': 0, 'level': 1, 'sort': id_counter
            })
            id_counter += 1
            for s in subs[r]:
                self.categories.append({
                    'id': id_counter, 'name': s, 'parent_id': root_id, 'level': 2, 'sort': id_counter
                })
                id_counter += 1

    def generate_recipes(self):
        print("Generating Recipes...")
        # Needs users to be generated first
        if not self.users:
            return

        for i in range(1, 501): # 500 recipes
            user = random.choice(self.users)
            cat = random.choice(self.categories)
            
            gmt_create = fake.date_time_between(start_date='-1y', end_date='now')
            
            rcp = {
                'id': i + 10000, # Start from 10000
                'author_id': user['id'],
                'title': get_chaos_string(fake.sentence(nb_words=4)),
                'cover_url': fake.image_url(),
                'video_url': "",
                'description': fake.text(max_nb_chars=100),
                'category_id': cat['id'],
                'cuisine_id': 0,
                'difficulty': random.randint(1, 5),
                'time_cost': random.choice([10, 30, 60, 120]),
                'calories': random.randint(100, 1000),
                'score': round(random.uniform(3.0, 5.0), 1),
                'status': 2, # Published
                'gmt_create': gmt_create.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
                'gmt_modified': gmt_create.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
                'is_deleted': 0,
                'version': 1
            }
            self.recipes.append(rcp)
            
            # Generate Stats
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
        print(f"Generating {USER_COUNT} users...")
        for i in range(1, USER_COUNT + 1):
            gmt_create = fake.date_time_between(start_date='-2y', end_date='now')
            last_login = fake.date_time_between(start_date=gmt_create, end_date='now')
            
            user = {
                'id': i,
                'username': get_chaos_string(fake.user_name() + str(i)), # Ensure unique base
                'mobile': fake.phone_number(),
                'email': get_chaos_string(fake.email()),
                'password_hash': fake.sha256(),
                'salt': fake.uuid4()[:32],
                'nickname': get_chaos_string(fake.name()),
                'avatar_url': get_chaos_string(fake.image_url()),
                'status': random.choices([1, 2, 3], weights=[80, 15, 5])[0], # 80% normal
                'register_source': random.choice(['APP', 'WEB', 'WX', 'MINIPROGRAM', 'UNKNOWN']),
                'register_ip': fake.ipv4(),
                'last_login_time': last_login.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
                'last_login_ip': fake.ipv4(),
                'gmt_create': gmt_create.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
                'gmt_modified': last_login.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3],
                'is_deleted': 0,
                'version': 1
            }
            # Profile fields (Doris dim_user_profile needs these)
            user['level'] = random.randint(1, 100)
            user['reg_time'] = user['gmt_create']
            user['total_spend'] = round(random.uniform(0, 10000), 2)
            
            self.users.append(user)

    def generate_transactions(self):
        print(f"Generating {TRANS_COUNT} transactions...")
        
        # Mapping for logical consistency
        # biz_type: 1-Recharge(Income), 2-Consume(Expense), 5-Commission(Income)
        # flow_type: 1-Income, 2-Expense
        BIZ_FLOW_MAP = {
            1: 1, # Recharge -> Income
            2: 2, # Consume -> Expense
            5: 1  # Commission -> Income
        }

        for i in range(1, TRANS_COUNT + 1):
            user = random.choice(self.users)
            amount = get_pareto_amount()
            biz_type = random.choice([1, 2, 5])
            flow_type = BIZ_FLOW_MAP.get(biz_type, 1)
            
            # Ensure transaction time is AFTER user registration
            user_reg_time = datetime.datetime.strptime(user['gmt_create'], "%Y-%m-%d %H:%M:%S")
            # If reg time is future relative to now (shouldn't happen with current logic but safe to check), use now
            if user_reg_time > datetime.datetime.now():
                user_reg_time = datetime.datetime.now() - datetime.timedelta(days=1)
                
            gmt_create_dt = fake.date_time_between(start_date=user_reg_time, end_date='now')
            gmt_create = gmt_create_dt.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
            
            # Extract date from gmt_create for sync accuracy
            stat_date = gmt_create_dt.strftime("%Y-%m-%d")
            
            trans = {
                'id': i,
                'transaction_no': uuid.uuid4().hex,
                'account_id': user['id'], 
                'biz_order_no': f"ORDER_{uuid.uuid4().hex[:12]}",
                'out_trade_no': f"PAY_{uuid.uuid4().hex[:16]}",
                'user_id': user['id'],
                'account_type': random.randint(1, 4),
                'flow_type': flow_type,
                'biz_type': biz_type,
                'amount': amount,
                'balance_after': round(Decimal(random.uniform(float(amount), 50000)), 2), # Ensure balance > amount generally
                'status': random.choices([0, 1, 2], weights=[10, 85, 5])[0], # 0-Processing, 1-Success, 2-Fail
                'remark': get_chaos_string(fake.sentence()),
                'gmt_create': gmt_create,
                'gmt_modified': gmt_create
            }
            self.transactions.append(trans)

            # Aggregate for Doris
            # Logic: Only successful transactions should probably count, but for chaos we might include all or just success.
            # Let's assume we aggregate ALL for checking, or just Success.
            # Usually settlement is for successful ones.
            if trans['status'] == 1:
                key = (stat_date, user['id'], biz_type) 
                if key not in self.daily_stats:
                    self.daily_stats[key] = {'total_amount': Decimal(0), 'trans_count': 0}
                self.daily_stats[key]['total_amount'] += amount
                self.daily_stats[key]['trans_count'] += 1

    def write_mysql_sql(self):
        with open(MYSQL_FILE, 'w', encoding='utf-8') as f:
            f.write("USE culinary_user;\n\n")
            
            # t_usr_base
            f.write("-- t_usr_base\n")
            for u in self.users:
                sql = f"INSERT INTO t_usr_base (id, username, mobile, email, password_hash, salt, nickname, avatar_url, status, register_source, register_ip, last_login_time, last_login_ip, gmt_create, gmt_modified, is_deleted, version) VALUES ({u['id']}, '{u['username']}', '{u['mobile']}', '{u['email']}', '{u['password_hash']}', '{u['salt']}', '{u['nickname']}', '{u['avatar_url']}', {u['status']}, '{u['register_source']}', '{u['register_ip']}', '{u['last_login_time']}', '{u['last_login_ip']}', '{u['gmt_create']}', '{u['gmt_modified']}', {u['is_deleted']}, {u['version']});\n"
                f.write(sql)
            
            # t_rcp_category
            f.write("\n-- t_rcp_category\n")
            for c in self.categories:
                sql = f"INSERT INTO t_rcp_category (id, name, parent_id, level, sort, is_visible) VALUES ({c['id']}, '{c['name']}', {c['parent_id']}, {c['level']}, {c['sort']}, 1);\n"
                f.write(sql)

            # t_rcp_info
            f.write("\n-- t_rcp_info\n")
            for r in self.recipes:
                # Find matching stats
                s = next((x for x in self.stats if x['recipe_id'] == r['id']), None)
                v_count = s['view_count'] if s else 0
                
                sql = f"INSERT INTO t_rcp_info (id, author_id, title, cover_url, description, category_id, cuisine_id, difficulty, time_cost, calories, score, view_count, like_count, collect_count, comment_count, share_count, try_count, status, gmt_create, gmt_modified, is_deleted, version) VALUES ({r['id']}, {r['author_id']}, '{r['title']}', '{r['cover_url']}', '{r['description']}', {r['category_id']}, {r['cuisine_id']}, {r['difficulty']}, {r['time_cost']}, {r['calories']}, {r['score']}, {v_count}, {s['like_count']}, {s['collect_count']}, {s['comment_count']}, {s['share_count']}, {s['try_count']}, {r['status']}, '{r['gmt_create']}', '{r['gmt_modified']}', {r['is_deleted']}, {r['version']});\n"
                f.write(sql)

            # t_rcp_stats
            f.write("\n-- t_rcp_stats\n")
            for s in self.stats:
                sql = f"INSERT INTO t_rcp_stats (recipe_id, view_count, like_count, collect_count, comment_count, share_count, try_count, score, gmt_modified) VALUES ({s['recipe_id']}, {s['view_count']}, {s['like_count']}, {s['collect_count']}, {s['comment_count']}, {s['share_count']}, {s['try_count']}, {s['score']}, '{s['gmt_modified']}');\n"
                f.write(sql)

            # t_fin_transaction_flow
            f.write("\n-- t_fin_transaction_flow\n")
            for t in self.transactions:
                sql = f"INSERT INTO t_fin_transaction_flow (id, transaction_no, account_id, biz_order_no, out_trade_no, user_id, account_type, flow_type, biz_type, amount, balance_after, status, remark, gmt_create, gmt_modified) VALUES ({t['id']}, '{t['transaction_no']}', {t['account_id']}, '{t['biz_order_no']}', '{t['out_trade_no']}', {t['user_id']}, {t['account_type']}, {t['flow_type']}, {t['biz_type']}, {t['amount']}, {t['balance_after']}, {t['status']}, '{t['remark']}', '{t['gmt_create']}', '{t['gmt_modified']}');\n"
                f.write(sql)

    def write_doris_sql(self):
        with open(DORIS_FILE, 'w', encoding='utf-8') as f:
            f.write("USE culinary_dw;\n\n")
            
            # dim_user_profile
            f.write("-- dim_user_profile\n")
            for u in self.users:
                sql = f"INSERT INTO dim_user_profile (user_id, nickname, level, reg_time, last_login_time, total_spend) VALUES ({u['id']}, '{u['nickname']}', {u['level']}, '{u['reg_time']}', '{u['last_login_time']}', {u['total_spend']});\n"
                f.write(sql)

            # dws_fin_daily_settlement (Aggregated)
            f.write("\n-- dws_fin_daily_settlement\n")
            for (date, merchant_id, biz_type), stats in self.daily_stats.items():
                sql = f"INSERT INTO dws_fin_daily_settlement (stat_date, merchant_id, biz_type, total_amount, trans_count) VALUES ('{date}', {merchant_id}, {biz_type}, {stats['total_amount']}, {stats['trans_count']});\n"
                f.write(sql)

            # dws_recipe_daily_stats (Aggregated)
            f.write("\n-- dws_recipe_daily_stats\n")
            # Generate one daily stat entry per recipe for 'today' (or the recipe's modified date)
            for r in self.recipes:
                # Find matching stats
                s = next((x for x in self.stats if x['recipe_id'] == r['id']), None)
                if s:
                    # Use gmt_modified date part as stat_date
                    stat_date = s['gmt_modified'].split(' ')[0]
                    # Randomize daily stats slightly less than total stats
                    daily_view = random.randint(0, s['view_count'])
                    daily_like = random.randint(0, s['like_count'])
                    daily_comment = random.randint(0, s['comment_count'])
                    daily_share = random.randint(0, s['share_count'])
                    
                    sql = f"INSERT INTO dws_recipe_daily_stats (stat_date, recipe_id, category_id, view_count, like_count, comment_count, share_count) VALUES ('{stat_date}', {r['id']}, {r['category_id']}, {daily_view}, {daily_like}, {daily_comment}, {daily_share});\n"
                    f.write(sql)

if __name__ == "__main__":
    factory = DataFactory()
    factory.generate_users()
    factory.generate_categories()
    factory.generate_recipes()
    factory.generate_transactions()
    factory.write_mysql_sql()
    factory.write_doris_sql()
    print(f"Done! Generated {MYSQL_FILE} and {DORIS_FILE}")
