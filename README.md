# DazBones公式HP

Java 17 / Spring Boot / MySQLで動作するチームサイトです。

## ローカル起動

既存のMySQLデータベースを起動し、`config/local.properties` に接続情報とログインコードを設定してください。このファイルはGit管理対象外です。

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/dazbones_db?serverTimezone=Asia/Tokyo
spring.datasource.username=dbuser
spring.datasource.password=YOUR_DB_PASSWORD
app.auth.admin-code=YOUR_ADMIN_CODE
app.auth.editor-code=YOUR_EDITOR_CODE
```

管理者と編集者には異なるコードを指定してください。未設定の権限ではログインできません。コードはUTF-8で72バイト以内にしてください。

```powershell
.\gradlew.bat bootRun
```

ブラウザで <http://localhost:8080/> を開きます。起動したターミナルのCtrl+Cで停止できます。

ローカル設定ファイルを置かない環境では `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`ADMIN_CODE`、`EDITOR_CODE`、必要に応じて `PORT`、`UPLOAD_DIR` を環境変数で設定できます。`config/local.properties` がある場合、そのファイルの同名設定が優先されます。

既存DBへの今回の移行は、バックアップ後に `./gradlew.bat bootRun --args="--spring.flyway.enabled=true"` で実行します。通常起動では移行は無効です。ローカルDBには実行済みです。空のDBを構築する初期DDLは含まれていません。詳しくは `docs/phase2-implementation.md` を参照してください。

## テスト

```powershell
.\gradlew.bat test
```

統合テストは専用のインメモリH2データベースを使い、ローカル設定ファイルを読み込みません。通常のMySQLデータには触れません。

## 認証

ログインは `/login` のフォームから行います。権限付与用の直接URLはありません。ログアウトと更新操作にはCSRFトークンが必要です。コードはDBにBCryptハッシュで保存します。設定ファイルの共有コードは初回登録時のみ使用し、画面で変更した管理者コードを再起動時に上書きしません。管理者・編集者はログインIDを空欄にします。メンバーは管理者が回答者管理で発行した本人用ID・コードを使用します。

以前のコード・DBパスワードはGit履歴には残ります。本番公開前の値の交換が必要です。今回のローカル設定移行では、既存環境との互換性のため値そのものは変更していません。

詳細な残課題は `docs/completion-review-2026-09-10.md`、今回の対応内容は `docs/implementation-progress-2026-09-10.md` を参照してください。
