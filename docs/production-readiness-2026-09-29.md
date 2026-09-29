# 本番公開までの進捗（2026-09-29）

## 完了・検証済み

- SNSリンク管理はmaster専用、タイトルは「SNS」。Instagram投稿の登録・削除は選手以上、並べ替えはmaster。
- Instagramは公式embed.jsでホーム内の写真表示を確認。直接投稿を開くリンクも設置。
- ログイン制限を実装。同一接続元の失敗・処理中リクエストを10分間で10回までとし、超過時は429とRetry-After、日本語案内を返す。成功した分だけ予約枠を返却し、別の失敗は消さない。並行リクエストも計数。実Caddy経由で偽装Forwarded／X-Forwarded-Forを変えても制限されることを検証。
- Caddyで利用者が渡したForwardedヘッダーを除去。接続元判定はアプリを直接公開せず、Caddy経由で利用する前提。
- Spring Boot 3.1.4 → 3.5.16で中間検証 → 4.0.8へ更新。Gradle 8.14.3、dependency-management 1.1.7、TwelveMonkeys 3.15.2、Caddy 2.11.4に更新。Jackson 3・テストAPI・404例外処理を移行。Tomcat 11.0.26、Jackson 2.21.6／3.1.6のセキュリティ修正も適用。
- Java 55件、JavaScript 9件成功。Dockerイメージ作成成功。
- 更新後の隔離Dockerで空DBのV0〜V5適用、ログイン、入力保存、DB・画像のバックアップ復元に成功。部費金額、Instagram投稿と並び順、site_settingsも復元確認。
- 実MySQL環境でmaster／選手の打撃成績・部費・道具・出欠・SNS・Instagram管理画面が200、readinessがUP。
- npm auditは既知脆弱性0件（実行時点）。Java・OSパッケージを含む総合脆弱性スキャンの完了を意味しない。
- Dockerログに10MB×3ファイルのローテーションを設定。ポリシー画面の旧権限名を修正。

## 承認済みのテストデータ整理

ユーザー確認後、名簿10人・部費9件・出欠17件・道具3件・お知らせ3件と旧アンケート・追加日付・操作履歴を初期化。SNS設定、Instagram投稿1件、画像、ログイン設定は保持。シーケンス番号はリセットしていない。

バックアップ: `backups/pre-production-cleanup-20260929-183101/`。DB全体、画像tar.gz、SHA-256一覧を保存。バックアップはGit対象外。機密情報を含むため公開しない。

## 残る必須作業

1. スマホ実機のSafari/Chromeと公開予定HTTPS環境で、Instagram・入力保存・画像アップロードを確認。CodexブラウザーとHTTP確認だけで実機検証済みとはしない。
2. Java・コンテナ全体の脆弱性スキャンと結果の対処。MySQL 8.4の本番イメージは配置直前に取得・確認。
3. 新しい実名簿・掲載内容を登録し、公開範囲を確認。問い合わせ先・運用担当者を決定。
4. 本番用のmaster／選手コード・DBパスワードを設定。既存DBを移行すると環境変数だけではDB内コードは変わらないため、コード変更画面で更新する。今回はログインコードを変更していない。
5. 今回のコミットのGitHub Actions成功を確認し、mainへ統合。
6. 公開先・費用・URLを決定し、Docker配置、DNS・HTTPS設定、公開URLでの最終確認。
7. 定期バックアップと別保管先、監視・通知先、更新・復元の担当を設定。既存スクリプトはあるが本番定期ジョブは未設定。

ログイン制限は単一アプリのメモリー内で保持し、再起動でリセットされる。複数台運用や分散した攻撃への対策には共有ストアや公開基盤側の制限が必要。同じ回線の利用者は枠を共有する。

## 依存更新の参照元

- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- https://docs.spring.io/spring-boot/4.0/system-requirements.html
- https://github.com/haraldk/TwelveMonkeys/releases
- https://github.com/caddyserver/caddy/releases