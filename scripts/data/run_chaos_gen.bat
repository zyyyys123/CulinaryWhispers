@echo off
echo Installing requirements...
pip install -r requirements.txt

echo Running chaos data generator...
python chaos_data_gen.py

echo Done. Check mysql_chaos_data.sql and doris_chaos_data.sql
pause
