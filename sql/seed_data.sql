-- Static seed data for CulinaryWhispers (Non-random)
-- 用于填充“无法随机生成/需要业务定义”的基础维度数据

USE culinary_user;

-- 内置账号（便于演示/测试）
INSERT INTO t_usr_base (id, username, mobile, email, password_hash, salt, nickname, avatar_url, status, register_source, register_ip, last_login_time, last_login_ip, is_deleted, version)
VALUES
  (9000000001, 'admin', '13000000000', 'admin@cw.local', '$2b$12$l5pFqsziKh1HHvdK76LQrOR44KdB9GSnPV3p71KSI6o7Ac0d8vxDm', 'seed_salt_admin_1234567890123456', '系统管理员', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', 1, 'WEB', '127.0.0.1', NOW(3), '127.0.0.1', 0, 1),
  (9000000002, 'demo',  '13100000001', 'demo@cw.local',  '$2b$12$l5pFqsziKh1HHvdK76LQrOR44KdB9GSnPV3p71KSI6o7Ac0d8vxDm', 'seed_salt_demo__1234567890123456', '演示用户',  'https://api.dicebear.com/7.x/avataaars/svg?seed=demo',  1, 'WEB', '127.0.0.1', NOW(3), '127.0.0.1', 0, 1)
ON DUPLICATE KEY UPDATE
  password_hash=VALUES(password_hash),
  salt=VALUES(salt),
  nickname=VALUES(nickname),
  avatar_url=VALUES(avatar_url),
  status=VALUES(status),
  last_login_time=VALUES(last_login_time),
  last_login_ip=VALUES(last_login_ip),
  is_deleted=VALUES(is_deleted),
  version=VALUES(version);

INSERT INTO t_usr_profile (user_id, gender, signature, country, province, city, occupation, interests, cook_age, favorite_cuisine, taste_preference, dietary_restrictions, vip_expire_time, vip_level, is_master_chef, master_title, bg_image_url, video_intro_url, contact_email, total_spend)
VALUES
  (9000000001, 0, '系统管理员账号', '中国', '北京', '北京', '管理员', '后台,运维,增长', 5, '家常菜', '清淡', '无', NULL, 3, 1, '平台运营官', 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1800&q=80', '', 'admin@cw.local', 0.00),
  (9000000002, 1, '演示账号：用于体验全站功能', '中国', '上海', '上海', '产品经理', '烘焙,咖啡,轻食', 2, '烘焙', '微甜', '无', NULL, 1, 0, NULL, 'https://images.unsplash.com/photo-1514511547113-baf87d9d6a3a?auto=format&fit=crop&w=1800&q=80', '', 'demo@cw.local', 0.00)
ON DUPLICATE KEY UPDATE
  gender=VALUES(gender),
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
  total_spend=VALUES(total_spend);

INSERT INTO t_usr_stats (user_id, level, experience, total_recipes, total_moments, total_likes_received, total_collects_received, total_fans, total_follows, total_views, week_active_days, month_active_days, last_publish_time)
VALUES
  (9000000001, 30, 250000, 12, 40, 8800, 4200, 5600, 120, 900000, 7, 30, NOW(3)),
  (9000000002, 8,  4200,   6,  12,  320,  180,  56,   18,  12000, 4,  12, NOW(3))
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
  last_publish_time=VALUES(last_publish_time);

-- 食谱分类（与脚本 chaos_data_gen.py 的分类口径保持一致）
INSERT INTO t_rcp_category (id, name, parent_id, level, sort, is_visible)
VALUES
  (1,  '家常菜', 0, 1, 1, 1),
  (2,  '烘焙',   0, 1, 2, 1),
  (3,  '西餐',   0, 1, 3, 1),
  (4,  '饮品',   0, 1, 4, 1),
  (5,  '川菜',   1, 2, 5, 1),
  (6,  '粤菜',   1, 2, 6, 1),
  (7,  '湘菜',   1, 2, 7, 1),
  (8,  '汤羹',   1, 2, 8, 1),
  (9,  '蛋糕',   2, 2, 9, 1),
  (10, '面包',   2, 2, 10, 1),
  (11, '饼干',   2, 2, 11, 1),
  (12, '牛排',   3, 2, 12, 1),
  (13, '意面',   3, 2, 13, 1),
  (14, '沙拉',   3, 2, 14, 1),
  (15, '果汁',   4, 2, 15, 1),
  (16, '茶饮',   4, 2, 16, 1),
  (17, '咖啡',   4, 2, 17, 1)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  parent_id = VALUES(parent_id),
  level = VALUES(level),
  sort = VALUES(sort),
  is_visible = VALUES(is_visible);

-- 基础标签（用于热门标签/推荐/搜索联想等；type: 1-通用, 2-食材, 3-场景）
INSERT IGNORE INTO t_rcp_tag (name, type, use_count) VALUES
  ('家常', 1, 0),
  ('下饭', 1, 0),
  ('快手', 1, 0),
  ('低脂', 1, 0),
  ('高蛋白', 1, 0),
  ('减脂餐', 1, 0),
  ('养生', 1, 0),
  ('宝宝辅食', 1, 0),
  ('鸡蛋', 2, 0),
  ('牛肉', 2, 0),
  ('鸡胸肉', 2, 0),
  ('土豆', 2, 0),
  ('番茄', 2, 0),
  ('虾仁', 2, 0),
  ('豆腐', 2, 0),
  ('西兰花', 2, 0),
  ('早餐', 3, 0),
  ('午餐', 3, 0),
  ('晚餐', 3, 0),
  ('宵夜', 3, 0),
  ('便当', 3, 0),
  ('聚会', 3, 0),
  ('露营', 3, 0);

-- 电商商品（用于前端 Market 页面展示）
INSERT INTO t_comm_product (title, description, price, stock, category_id)
VALUES
  ('不粘锅 28cm', '家用不粘锅，少油烟，适合煎炒。', 129.00, 320, 1),
  ('厨师刀 8英寸', '高碳钢厨师刀，切片顺滑。', 89.90, 210, 1),
  ('空气炸锅 4L', '低油烹饪，适合薯条/鸡翅/烘焙。', 299.00, 120, 2),
  ('电子秤 5kg', '烘焙称重，0.1g 精度。', 39.90, 650, 2),
  ('咖啡手冲壶', '细口壶控流稳定，适合手冲。', 79.00, 260, 3),
  ('料理机 1000W', '多档搅拌，制作奶昔/辅食。', 199.00, 180, 3),
  ('零基础家常菜入门课', '30 天打卡：刀工/火候/调味系统训练。', 99.00, 9999, 4),
  ('烘焙进阶课程 · 司康&面包', '从配方到发酵：一次掌握烘焙底层逻辑。', 199.00, 9999, 4),
  ('CulinaryWhispers 围裙', '加厚帆布防油污，双口袋。', 59.00, 800, 5),
  ('厨房贴纸套装', '16 张防水贴，装饰冰箱/杯子。', 19.90, 2000, 5);
