# DazBones公式HP

Java 17 / Spring Boot / MySQLで動作するチームサイトです。

## ローカル起動

既存のMySQLデータベースを起動し、`config/local.properties` に接続情報とログインコードを設定してください。このファイルはGit管理対象外です。

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/dazbones_db?serverTimezone=Asia/Tokyo
spring.datasource.username=dbuser
spring.datasource.password=YOUR_DB_PASSWORD
app.auth.master-code=YOUR_MASTER_CODE
app.auth.player-code=YOUR_PLAYER_CODE
```

masterと選手には異なるコードを指定してください。未設定の権限ではログインできません。コードはUTF-8で72バイト以内にしてください。

```powershell
.\gradlew.bat bootRun
```

ブラウザで <http://localhost:8080/> を開きます。起動したターミナルのCtrl+Cで停止できます。

ローカル設定ファイルを置かない環境では `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`ADMIN_CODE`、`EDITOR_CODE`、必要に応じて `PORT`、`UPLOAD_DIR` を環境変数で設定できます。`config/local.properties` がある場合、そのファイルの同名設定が優先されます。

DB移行は、バックアップ後に `./gradlew.bat bootRun --args="--spring.flyway.enabled=true"` で実行します。通常のローカル起動では移行は無効です。V0に空DB用の初期スキーマ、V1以降に差分移行を含みます。既存のbaseline 0環境ではV0は実行されません。ローカルDBはV5まで適用済みです。

## 公開用Docker構成・画面資産

公開環境の起動、HTTPS、バックアップ・復元は [運用手順](docs/deployment.md) を参照してください。公開先は未定で、実サーバーへの公開は行っていません。

CSSとFullCalendarはローカル配信します。テンプレートやスタイルを変更した際は `npm ci --ignore-scripts`、`npm run build` で資産を再生成してください。生成物はGit管理しているため、通常のGradle起動にNode.jsは不要です。

最新の権限・一括入力・部費・出欠確認の仕様は [現行仕様](docs/shared-input-spec.md) を参照してください。

以前の①②③の変更と検証結果は [実装メモ](docs/phase3-implementation.md) にまとめています。

## テスト

```powershell
.\gradlew.bat test
npm test
```

統合テストは専用のインメモリH2データベースとテスト用認証コードを明示して実行します。通常のMySQLデータには触れません。

## 認証

ログインは `/login` のフォームから行います。権限付与用の直接URLはありません。ログアウトと更新操作にはCSRFトークンが必要です。コードはDBにBCryptハッシュで保存します。設定ファイルの共有コードは初回登録時のみ使用し、画面で変更した管理者コードを再起動時に上書きしません。masterと選手は共通コードのみでログインします。旧管理者・編集者・本人用IDは利用できません。`ADMIN_CODE` はmaster、`EDITOR_CODE` は選手の初期コードに対応します。

以前のコード・DBパスワードはGit履歴には残ります。本番公開前の値の交換が必要です。今回の共有ログインコードはローカル設定にのみ保存し、Gitへは含めていません。

詳細な残課題は `docs/completion-review-2026-09-10.md`、今回の対応内容は `docs/implementation-progress-2026-09-10.md` を参照してください。
