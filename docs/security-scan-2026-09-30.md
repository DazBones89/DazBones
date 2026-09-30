# 公開用イメージ再スキャン（2026-09-30）

`deploy/Scan-Images.ps1 -OutputDirectory build/audit`を実行。Trivyはスクリプト内のdigestで固定し、無視リストなしで全重大度を集計した。イメージID・件数は同日JSONに保存。詳細はローカル`build/audit/site.json`、`proxy.json`、`mysql.json`。

| イメージ | Critical | High | Medium | Low | Unknown |
|---|---:|---:|---:|---:|---:|
| アプリ | 0 | 0 | 13 | 16 | 0 |
| Caddy | 0 | 0 | 1 | 0 | 1 |
| MySQL | 0 | 0 | 0 | 0 | 1 |

前回のCaddy High 17件、MySQL Critical 1件・High 37件は今回の構成で検出されなくなった。スキャンは既知情報との照合であり、脆弱性がないことの保証ではない。

## 残る指摘と扱い

- アプリOSのMedium 13件・Low 16件はスキャン時点で修正版未提示。ベースイメージ更新時に再評価する。
- CaddyのCEL `GHSA-gcjh-h69q-9w9g`はMedium。0.29.0への更新ではCaddy 2.11.4のNewCall APIが非互換となりビルド不能のため、0.28.1を維持。現CaddyfileはCEL式を使用せず、利用者が式を登録する機能も設けていない。設定変更時とCaddy更新時に再評価する。[アドバイザリ](https://github.com/advisories/GHSA-gcjh-h69q-9w9g)はNativeTypesとParseStructTagによるJSON非公開フィールドの扱いを説明している。
- Caddyの`GO-2026-5932`はUnknown。x/crypto/openpgpが未保守である指摘で、修正版の提示なし。現サイトにOpenPGP機能はないが、依存内の到達可能性まで確認済みとはしていない。依存更新時に追跡する。
- MySQLのgosuに含まれるx/sysの`CVE-2026-39824`はUnknown。WindowsのNewNTUnicodeStringに関する指摘で、今回のLinuxコンテナではWindows用APIを実行しない。gosuの依存更新時にも再確認する。

## 保守

CIで3イメージを毎回スキャンし、High/Criticalは失敗扱い、全重大度のJSONを保存する。Medium以下・Unknownは自動で安全扱いにせず、公開前レビューと更新時レビューを継続する。
