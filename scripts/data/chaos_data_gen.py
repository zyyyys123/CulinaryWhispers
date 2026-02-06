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
        self.daily_stats = {} # (date, merchant_id, biz_type) -> {amount, count}

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
                # Handle None/Nulls for SQL
                sql = f"INSERT INTO t_usr_base (id, username, mobile, email, password_hash, salt, nickname, avatar_url, status, register_source, register_ip, last_login_time, last_login_ip, gmt_create, gmt_modified, is_deleted, version) VALUES ({u['id']}, '{u['username']}', '{u['mobile']}', '{u['email']}', '{u['password_hash']}', '{u['salt']}', '{u['nickname']}', '{u['avatar_url']}', {u['status']}, '{u['register_source']}', '{u['register_ip']}', '{u['last_login_time']}', '{u['last_login_ip']}', '{u['gmt_create']}', '{u['gmt_modified']}', {u['is_deleted']}, {u['version']});\n"
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

if __name__ == "__main__":
    factory = DataFactory()
    factory.generate_users()
    factory.generate_transactions()
    factory.write_mysql_sql()
    factory.write_doris_sql()
    print(f"Done! Generated {MYSQL_FILE} and {DORIS_FILE}")
