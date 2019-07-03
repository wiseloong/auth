# wise-auth

> 认证登陆组件

## 引用

```clojure
; 前端项目
; 引用组件包
:dependencies [[wiseloong/auth "0.1.0-SNAPSHOT"]]
; 引入文件
(:require [wise.auth.core :as auth])
```

```clojure
; 后端项目
; 引用组件包
:dependencies [[wiseloong/auth "0.1.0-SNAPSHOT"]
               [metosin/compojure-api "2.0.0-alpha25"]]
; 引入文件
(:require [wise.auth.core :as auth]
          [wise.auth.middleware :refer [token-auth-mw authenticated-mw]])
```

## 前端使用

### 普通登陆

1. 配置

``` clojure
(def config {:service "http://localhost:3000/"})
```

2. 使用

```clojure
;; 普通登陆
(defn logout []
  (auth/logout store/clear-user))

(defn login [main]
  [auth/login main config set-metadata set-user error-handler])

;; 定时半小时刷新认证信息
(defonce time-updater (js/setInterval #(auth/refresh config) 300000))
```

### oauth登陆

1. 配置

```clojure
(def config {:login-type #{:oauth2 :base}
             :oauth2     {:provider     {:uri               "http://192.168.17.26/"
                                         :authorization-uri "cas/oauth2.0/authorize"
                                         :token-uri         "cas/oauth2.0/accessToken"
                                         :user-info-uri     "cas/oauth2.0/profile"
                                         :logout-uri        "cas/logout"}
                          :registration {:provider      "wise-sso"
                                         :client-id     "localcastest"
                                         :client-secret "localcastest"
                                         :redirect-uri  "http://127.0.0.1:3449/"
                                         :scope         "simple"
                                         :client-name   "hrms"}}
             :service    "http://127.0.0.1:3000/"})
```

2. 使用

```clojure
;; 对接oauth2认证登陆
(defn logout []
  (auth/oauth-logout config))

(defn login [main]
  [auth/oauth-login main (:route @route-state) config
   store/set-metadata store/clear-user store/set-user ajax/error-handler])

;; 定时半小时刷新认证信息
(defonce time-updater (js/setInterval #(auth/refresh config) 300000))

```

## 后端使用

```clojure
;; oauth获取用户id
(defn oauth-user-id [data]
  (try
    (oauth/oauth-wise-user-id data)
    (catch Exception e
      (return-warn true "认证失败！请重新登陆！"))))
   
   
    (POST "/login" []
      :body-params [username :- s/Str, password :- s/Str]
      :summary "根据id获取用户信息"
      (let [user (adb/find-user username password)
            user (dissoc user :password)
            token (auth/sign-token user)]
        (log/info (:code user) "-" (:name user) "登陆系统")
        (ok (merge user {:token token}))))

    (POST "/oauth-login" []
      :body [data {s/Keyword s/Any}]
      :summary "根据id获取用户信息"
      (let [user-id (adb/oauth-user-id data)
            user (adb/find-user user-id)
            user (dissoc user :password)
            token (auth/sign-token user)]
        (log/info (:code user) "-" (:name user) "登陆系统")
        (ok (merge user {:token token}))))

    (POST "/refresh-token" []
      :current-user user
      :summary "根据id获取用户信息"
      (log/info (:code user) "-" (:name user) "刷新了权限")
      (if user
        (let [token (auth/sign-token user)]
          (ok {:token token}))
        (bad-request {:msg "用户信息失效，请重新登陆！"})))
```
