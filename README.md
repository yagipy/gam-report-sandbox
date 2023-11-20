## 実行
- `ads.properties`を作成し、以下のように記述する
```shell
api.admanager.applicationName=<APPLICATION_NAME>
api.admanager.networkCode=<NETWORK_CODE>
```

- `SERVICE_ACCOUNT_JSON_KEY`を環境変数に設定する
```shell
export SERVICE_ACCOUNT_JSON_KEY=<SERVICE_ACCOUNT_JSON_KEY>
```

- 実行
```shell
sbt run
```
