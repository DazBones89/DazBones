# DazBones 開発ガイド（ローカル画面 / コマンド / CRUD仕様）

このドキュメントは、DazBones のローカル開発で使用する
URL・コマンド・Player機能仕様をまとめたもの。

---

# 1. 作業フォルダ確認

## 1.1 正しいディレクトリ

作業は必ず：C:\GitHub\DazBones

```powershell
cd C:\GitHub\DazBones
```

------
# 2. Spring Boot / Gradle コマンド
## 2.1 ビルド
```powershell
.\gradlew.bat clean build
```
## 2.2 起動
```powershell
.\gradlew.bat bootRun
```
## 2.3 daemon無効で起動（トラブル時）
```powershell
.\gradlew.bat bootRun --no-daemon
```
## 2.4 Gradle停止
```powershell
.\gradlew.bat --stop
```
## 2.5 キャッシュ削除（トラブル時）
```powershell
.\gradlew.bat --stop
Remove-Item -Recurse -Force .\build
Remove-Item -Recurse -Force .\.gradle
.\gradlew.bat build --no-daemon
```

------
# 3. Docker（MySQL）コマンド
## 3.1 コンテナ確認
```powershell
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```
## 3.2 MySQL接続
```powershell
docker exec -it dazbones-mysql mysql -u dbuser -p
```
## 3.3 DB確認
```powershell
SHOW DATABASES;
USE dazbones_db;
SHOW TABLES;
```
## 3.4 players確認
```powershell
docker exec -it dazbones-mysql mysql -u dbuser -p -e "USE dazbones_db; SELECT id,name,number FROM players ORDER BY id DESC LIMIT 5;"
```

------
# 4. Gitコマンド
## 4.1 状態確認
```powershell
git status
git log --oneline -5
```
## 4.2 通常Push
```powershell
git add .
git commit -m "message"
git push origin main
```
## 4.3 non-fast-forward時
```powershell
git pull --rebase origin main
git push origin main
```

------
# 5. ローカル画面URL一覧
## 5.1トップ
#### http://localhost:8080/
#### http://localhost:8080/main

## 5.2 ログイン
#### http://localhost:8080/login

## 5.3 選手
#### 一覧： http://localhost:8080/player
#### 追加： http://localhost:8080/player/add
#### 編集： http://localhost:8080/player/edit/{id}



