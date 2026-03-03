@echo off
echo Installing requirements...
pip install -r requirements.txt

echo Running chaos data generator...
python chaos_data_gen.py

echo Done. Check .\out\mysql_chaos_data.sql and .\out\doris_chaos_data.sql
pause
