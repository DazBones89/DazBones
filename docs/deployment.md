# Docker公開・バックアップ運用

## 構成と前提

Docker ComposeでJavaアプリ、MySQL 8.4、Caddyを起動する。公開先は未定のため、設定とローカル検証までを実施した。実ドメインでの証明書発行や外部からの疎通は、サーバー選定後に確認する。

Docker Engine / Docker Desktop、Compose v2、PowerShell 7が必要。画像・DB・証明書は別々の名前付きボリュームに永続化する。DBとアプリのポートはホストへ直接公開せず、Caddyの80/443だけ公開する。

## 初回起動

リポジトリのルートで実行する。`team.example.com` は取得済みの実ドメインに置き換え、DNSをサーバーへ向けて80/443を開通させる。

```powershell
./deploy/Initialize-Environment.ps1 -SiteHost team.example.com
docker compose --project-name dazbones --env-file deploy/.env -f deploy/compose.yml build --pull --no-cache
docker compose --project-name dazbones --env-file deploy/.env -f deploy/compose.yml up -d
```

初期化スクリプトは新しいランダムなDBパスワード、master・選手コードを `deploy/.env` に生成する。既存ファイルは上書きしない。このファイルはGit管理対象外とし、アクセスできるOSユーザーを運用担当者に限定して別途保管する。`.env.example` の仮文字列のままでは本番起動できない。

空DBにはFlyway V0〜V5を適用する。既存のFlyway管理済みDBでは未適用の移行だけ実行する。本番は `baseline-on-migrate=false` のため、未管理の既存DBを誤って初期化しない。旧環境を移す場合は、まず旧環境のスキーマと移行履歴を確認する。

`https://実ドメイン/health/readiness` が `{"status":"UP"}` になり、ログイン、名簿、画像表示を確認できたら利用を開始する。Caddyが証明書の発行・更新を管理する。本番のセッションCookieはSecure/HttpOnly/SameSite=Lax。

## 更新と障害確認

```powershell
./deploy/Backup.ps1
git pull --ff-only
docker compose --project-name dazbones --env-file deploy/.env -f deploy/compose.yml build --pull --no-cache
docker compose --project-name dazbones --env-file deploy/.env -f deploy/compose.yml up -d
docker compose --project-name dazbones --env-file deploy/.env -f deploy/compose.yml ps
docker compose --project-name dazbones --env-file deploy/.env -f deploy/compose.yml logs --tail 100 app db proxy
```

更新後にヘルスチェックと主要画面を確認する。DB移行後に旧アプリへ戻す場合は、旧スキーマとの互換性を確認し、必要なら更新前のDB・画像をセットで復元する。移行SQLを後から書き換えてチェックサムエラーを回避してはいけない。

## バックアップ

```powershell
./deploy/Backup.ps1 -Keep 5
```

バックアップ中はアプリを停止し、DBと画像を同じ時点の状態で保存した後、元々起動中なら再開する。短時間のメンテナンス時間が必要。DBは稼働させたままtransaction付きで取得する。

保存先は `deploy/backups/backup-日時-ID/`。SQL、画像tar.gz、SHA-256付きmanifestをセットで保存する。完全なバックアップの作成後だけ古い世代を削除し、標準で5世代を保持する。失敗したバックアップは完了扱いにしない。バックアップ先の別ディスク・別ホストへのコピーは運用側で設定する。

定期取得の初期案は週1回・5世代。公開サーバーで実行時刻を確定後、LinuxのcronやWindowsタスクスケジューラに、リポジトリを作業ディレクトリとして次のコマンドを登録する。現在のPCには定期ジョブを登録していない。

```text
pwsh -NoProfile -File /配置先/DazBones/deploy/Backup.ps1 -Keep 5
```

## 復元

復元先の既存DB・画像を置き換えるため、必要な現行データを先にバックアップする。`-ConfirmReplace` を指定しない場合は何も変更しない。

```powershell
./deploy/Restore.ps1 -BackupDirectory ./deploy/backups/backup-対象日時-ID -ConfirmReplace
```

チェックサムとアーカイブ内のパスを検証してから復元する。失敗時はアプリを停止したままにする。完了後はヘルスチェック、ログイン、選手と部費、出席回答、画像を確認する。

ログインコードのハッシュもDBバックアップに含まれるため、復元後はバックアップ時点のコードが有効になる。環境変数のコードは初回登録にしか使わない。過去環境の管理者コードを交換するときは画面のコード変更を使用し、編集者コードやDBユーザーの交換は管理された保守作業として別途行う。

## 再現テストとCI

```powershell
docker build -t dazbones-site:local .
docker build -f deploy/mysql.Dockerfile -t dazbones-mysql:local .
docker build -f deploy/proxy.Dockerfile -t dazbones-proxy:local .
./deploy/Test-Deployment.ps1
./deploy/Scan-Images.ps1
```

このテストは固有名の検証用Compose環境を2つ作り、空DB移行、ログイン、名簿表示、DB・画像のバックアップと別環境への復元を確認する。終了時にその2つの検証環境だけを削除する。既存の `dazbones-mysql` には接続しない。ポート19080/19081/19443/19444を使用するため、空けておく。

HTTPのローカル検証中だけ専用overrideでSecure Cookieを解除する。本番設定は変更しない。検証用の環境ファイル・バックアップはGit管理外の `build/deployment-check` に作成する。

GitHub ActionsはPush/PRで、CSS生成差分、Java・JavaScriptテスト、JARビルド、3種類のDockerビルド、同じ復元テスト、脆弱性スキャンを実行する。High/Criticalが検出された場合は失敗し、JSONレポートを成果物として保存する。リポジトリ側でActionsの実行が有効であることが前提。

## 公開用イメージの保守

MySQLは公式8.4をベースにOSを更新し、未使用のMySQL Shellと同梱Pythonを除去する。mysql/mysqldumpは保持し、起動時の権限切り替えに使うgosu 1.19をGo 1.26.6で再ビルドする。Caddy 2.11.4は標準モジュールを含め、deploy/proxy/go.mod・go.sumで依存を固定して同じGoでビルドする。

更新時は全イメージを再ビルドして復元テストとスキャンを実施する。スキャン結果はbuild/security-scanに保存される。Medium/Low/Unknownもレビュー対象であり、High/Criticalが0件でも安全性の保証にはならない。CEL 0.29.0は現CaddyとAPI互換性がないため0.28.1を維持し、残る指摘はsecurity-scan-2026-09-30.mdに記録する。


## 2026-09-29の運用補足

ログイン失敗は同一接続元あたり10分間で10回まで。超過時は429とRetry-Afterを返す。枠は単一アプリのメモリー内にあり再起動で解除される。Caddyは利用者指定のForwardedヘッダーを除去する。アプリを直接公開せず、追加のCDN・プロキシを置く場合は信頼する接続元設定を再点検する。

DB・app・proxyのDockerログは各10MB×3ファイルまで保持する。定期バックアップと外部保管・通知先は公開先決定後に設定する。
