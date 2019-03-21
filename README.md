# 期权SDK接入文档  

> 当前版本 v0.1.0

## 目录

- [一、SDK接入概述](#sdk接入概述)
- [二、服务端接口说明](#服务端接口说明)
- [三、Android集成文档](#android集成文档)
- [四、iOS集成文档](#ios集成文档)
- [五、Web集成文档](#web集成文档)

# SDK接入概述

### 接入流程概述

1. 申请ApiKey/ApiSecret（开发环境）
2. 服务端接入
3. APP集成SDK
4. 测试
5. 切换正式环境（ApiKey/ApiSecret，SDK）

### 服务端接入流程

1. 申请测试环境apikey/apisecret(在商务合作达成后由FOTA提供)，后续api使用上述秘钥签名，签名方式见 [apisecret签名](#apisecret签名)
2. 服务端在用户开通期权或者注册时，同步调用fota的api [新建券商子账号](#新建券商子账号)，fota将返回一个uid和password，后台需保存账号间的映射关系，后续登录时使用
3. 用户登录时使用上述接口返回的uid和password，调用[券商子账号登录](#券商子账号登录)，登录成功得到token，该token需要在客户端SDK设置，该token的默认有效期为7天，券商可自行设置
4. 调用资金的转入转出接口，具体见接口文档
5. 在SDK下单
6. 资金往来的流水查询，具体见接口文档
7. 交易记录的信息查询，具体见接口文档

### APP接入流程

1. 集成对应平台的SDK
2. 设置必要的参数和回调
3. demo工程下载地址

> https://github.com/FotaPodRepo

### 开发环境

> SDK接入默认使用开发环境，接入完成后参照对应平台的SDK接入文档修改为线上生产环境。

# 服务端接口说明

## 目录

- [接入环境](#接入环境)
- [apisecret签名](#apisecret签名)
- [服务器时间](#服务器时间)
- [新建券商子账号](#新建券商子账号)      
- [券商子账号登录](#券商子账号登录)   
- [券商子账号登出](#券商子账号登出)     
- [资金转入](#资金转入)   
- [资金转出](#资金转出)   
- [资金转入转出查询](#资金转入转出查询)  
- [资金转入转出分页查询](#资金转入转出分页查询) 
- [完整交易记录](#完整交易记录) 
- [券商单个子账号余额查询](#券商单个子账号余额查询)
- [券商所有子账号余额查询](#券商所有子账号余额查询) 
- [券商支持币种查询](#券商支持币种查询) 
- [券商子账号查询](#券商子账号查询) 
- [公共错误码](#公共错误码)

### 接入环境

> 默认使用开发环境，开发环境服务端前缀为`https://api-test.fota.com/mapi/`，正式环境服务端前缀为`https://api-option.fota.com/mapi/`

以下在介绍接口时统一使用 url 代替该前缀。

### apisecret签名

**签名前准备的数据：**

GET参数排序 + POST_BODY里的JSON字符串 + TIMESTAMP值(+表示字符串连接)
连接完成后，进行 Base64 编码，对编码后的数据进行HMAC SHA256签名（密钥为apisecret的值），并对签名进行二次 Base64 编码。

**各部分解释：**

- 参数排序

GET请求时，所有请求的 key 按照字母顺序排序，如果首字母相同，则按照第二个字母，以此类推。

- POST_BODY字符串

post_body是指请求主体的字符串(无需排序)，如果请求没有主体(通常为GET请求)则body可省略。

- TIMESTAMP

访问 API 时的时间戳，精确到毫秒。需要和服务器之间的时间差少于 30 秒，时间戳和服务器时间相差30秒以上的请求将被系统视为过期并拒绝。

**示例：**

对于如下请求：

```
GET orders?type=limit&amount=100&price=500
```

header里面包含的值如下:

```
apikey: "fota"
timestamp: "1523069544359"
signature: "OThkMDhlMGM0OGI2YTRkNDMyMmYxODc2NjI0YjJhZGJkMWI4ZWM3N2FlYTljM2YzZGNmYWQ0NjVmNDRhY2JiZA=="
```

signature结果生成步骤如下：可以查看[SignatureDemoTest.java](https://github.com/FotaPodRepo/optionSDK-service/blob/master/src/test/java/com/fota/optionSDKservice/SignatureDemoTest.java)

1. 准备需要签名的数据
2. 进行 Base64 编码
3. 使用apisecret进行HMAC SHA256签名
4. 再次进行 Base64 编码

POST请求（JAVA，以资金转入为例）：

```
final String apikey = "返回给你的apikey";
final String apisecret = "返回给你的apisecret";

String timestamp = String.valueOf(System.currentTimeMillis());
String requestBody = "{\"userId\" : \"1547694206089\",\"amount\" : \"12\",\"assetId\" : 2,\"assetName\": \"BTC\",\"serialNum\" : \"1111111\"}" ;
String stringForSigning = requestBody + timestamp;
// 进行加密
String signature = Base64.encodeBase64String(new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret).hmacHex(Base64.encodeBase64String(stringForSigning.getBytes(StandardCharsets.UTF_8))).getBytes());
```

GET（JAVA，以转账查询为例）：

```
final String apikey = "返回给你的apikey";
final String apisecret = "返回给你的apisecret";

String timestamp = String.valueOf(System.currentTimeMillis());
// 需要按照key进行排序
String requestGet = "serialNum=1111112&userId=1547694206089";
String stringForSigning = requestGet + timestamp;
// 进行加密
String signature = Base64.encodeBase64String(new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret).hmacHex(Base64.encodeBase64String(stringForSigning.getBytes(StandardCharsets.UTF_8))).getBytes());
```

**注意：对于java中的HmacAlgorithms和HmacUtils，请使用commons-codec-1.11版本及以上版本**

### 服务器时间

**简要描述：** 

- 获取当然的服务器时间，在做签名校验时需要比对时间差。该接口不需要做签名。

**请求URL：** 

- url + `v1/option/time`

**请求方式：**

- GET

**参数：** 无

 **返回示例**

```
{
  "code" : 0,
  "data" : {
    "time" : 1550813986210
  },
  "success" : true,
  "msg" : null
}
```

 **返回参数说明** 

| 参数名 | 类型 | 说明                 |
| :----- | :--- | -------------------- |
| time   | Long | 服务器当前毫秒的时间 |

### 新建券商子账号   

**简要描述：** 

- 新建券商子账号

**请求URL：** 

- url + `v1/users/subaccount/add`

**请求方式：**

- POST 

**参数：** 

| 参数名 | 必选 | 类型   | 说明                               |
| :----- | :--- | :----- | ---------------------------------- |
| suid   | 是   | String | 第三方平台的用户id                 |
| email  | 否   | String | 用户邮箱，email和phone必须要有一个 |
| phone  | 否   | String | 用户手机号                         |
| name   | 否   | String | 昵称                               |

 **返回示例**

```
{
  "code" : 0,
  "data" : {
    "uid" : "222222",
    "password" : "123456",
    "suid" : "15531"
  },
  "success": true
}
```

 **返回参数说明** 

| 参数名   | 类型   | 说明               |
| :------- | :----- | ------------------ |
| uid      | String | 用户id             |
| password | String | 为用户生成的密码   |
| suid     | String | 第三方平台的用户id |

### 券商子账号登录

**简要描述：** 

- 券商子账号登录

**请求URL：** 

- url + `v1/users/subaccount/login`

**请求方式：**

- POST 

**参数：** 

| 参数名           | 必选 | 类型   | 说明                                                 |
| :--------------- | :--- | :----- | ---------------------------------------------------- |
| userId           | 是   | String | 用户id，即上面返回的uid                              |
| password         | 是   | String | 用户密码                                             |
| tokenFailureTime | 否   | Long   | token的有效时间，单位为s，默认是7天（3600 * 24 * 7） |

 **返回示例**

```
{
  "code" : 0,
  "data" : {
    "token" : "111111"  
  },
  "success": true
}
```

 **返回参数说明** 

| 参数名 | 类型   | 说明                                 |
| :----- | :----- | ------------------------------------ |
| token  | String | 第三方平台需要自己保存，后面做签名用 |



### 券商子账号登出    

**简要描述：** 

- 券商子账号登出，删除token

**请求URL：** 

- url + `v1/users/subaccount/logout`

**请求方式：**

- POST 

**参数：** 

| 参数名 | 必选 | 类型   | 说明   |
| :----- | :--- | :----- | ------ |
| userId | 是   | String | 用户id |

 **返回示例**

```
{
  "code" : 0,
  "msg" : "",
  "success": true
}
```

 **返回参数说明** 

|参数名|类型|说明|
|:-----  |:-----|-----                           |

### 资金转入

**简要描述：** 

- 资金转入

**请求URL：** 

- url + `v1/users/subaccount/deposit`

**请求方式：**

- POST 

**参数：** 

| 参数名    | 必选 | 类型    | 说明                 |
| :-------- | :--- | :------ | -------------------- |
| userId    | 是   | String  | 用户id               |
| amount    | 是   | String  | 划转金额             |
| assetId   | 是   | Integer | 币的id               |
| assetName | 是   | String  | 币的名称             |
| serialNum | 是   | String  | 流水号，不可重复使用 |

 **返回示例**

```
{
    "msg": null,
    "code": 0,
    "data": null,
    "success": true
}
```

 **返回参数说明** 

|参数名|类型|说明|
|:-----  |:-----|-----|

### 资金转出

**简要描述：** 

- 资金转出

**请求URL：** 

- url + `v1/users/subaccount/withdraw`

**请求方式：**

- POST 

**参数：** 

| 参数名    | 必选 | 类型    | 说明                 |
| :-------- | :--- | :------ | -------------------- |
| userId    | 是   | String  | 用户id               |
| amount    | 是   | String  | 划转金额             |
| assetId   | 是   | Integer | 币的id               |
| assetName | 是   | String  | 币的名称             |
| serialNum | 是   | String  | 流水号，不可重复使用 |

 **返回示例**

```
{
    "msg": null,
    "code": 0,
    "data": null,
    "success": true
}
```

 **返回参数说明** 

|参数名|类型|说明|
|:-----  |:-----|-----|

### 资金转入转出查询

**简要描述：** 

- 资金转入转出查询（用于对账），一次最多返回1000条数据

**请求URL：** 

- url + `v1/users/subaccount/asset`

**请求方式：**

- GET

**参数：** 

| 参数名    | 必选 | 类型   | 说明                         |
| :-------- | :--- | :----- | ---------------------------- |
| userId    | 是   | String | 用户id                       |
| serialNum | 否   | String | 流水号                       |
| assetIds  | 否   | String | 币种ids，以逗号分割(示例1,2) |

 **返回示例**

```
{
    "msg": null,
    "code": 0,
    "data": [
        {
            "userId": "2508706974418994176",
            "brokerId": "13",
            "assetId": 2,
            "assetName": "BTC",
            "amount": "100.0000000000000000",
            "serialNum": "1112221",
            "type": 1,
            "time": 1551131245000
        },
        {
            "userId": "2508706974418994176",
            "brokerId": "13",
            "assetId": 2,
            "assetName": "BTC",
            "amount": "100.0000000000000000",
            "serialNum": "1112222221",
            "type": 1,
            "time": 1551131443000
        }
    ],
    "success": true
}
```

 **返回参数说明** 

| 参数名    | 类型    | 说明                    |
| :-------- | :------ | ----------------------- |
| brokerId  | String  | 券商id                  |
| amount    | String  | 划转金额                |
| serialNum | String  | 流水号                  |
| userId    | String  | 用户id                  |
| assetId   | Integer | 币种id                  |
| assetName | String  | 币种名称                |
| type      | Integer | 划转类型:1 转入，2 转出，3 结算 |
| time      | Long    | 创建时间                |


### 资金转入转出分页查询
**简要描述：** 

- 资金转入转出分页查询

**请求URL：** 

- url + `v1/users/subaccount/asset/page`

**请求方式：**

- GET

**参数：** 

| 参数名    | 必选 | 类型   | 说明                         |
| :-------- | :--- | :----- | ---------------------------- |
| userId    | 是   | String | 用户id                       |
| serialNum | 否   | String | 流水号                       |
| assetIds  | 否   | String | 币种ids，以逗号分割(示例1,2) |
| pageNum   | 否   | Integer | 页码，默认1                                           |
| pageSize  | 否   | Integer | 每页条数，默认20，默认按照创建时间倒序 |

 **返回示例**

```
{
    "msg":null,
    "code":0,
    "data":
    {
        "pageNo":1,
        "pageSize":3,
        "total":781,
        "item":[
            {
                "userId":"2509065103023670272",
                "brokerId":"119",
                "assetId":2,
                "assetName":"BTC",
                "amount":"100.0000000000000000",
                "serialNum":"100010122344519109",
                "type":1,
                "time":1552373219000
            },
            {
                "userId":"2509065103023670272",
                "brokerId":"119",
                "assetId":2,
                "assetName":"BTC",
                "amount":"-100.0000000000000000",
                "serialNum":"123411004533322444",
                "type":2,
                "time":1552372377000
            },
            {
                "userId":"2509065103023670272",
                "brokerId":"119",
                "assetId":2,
                "assetName":"BTC",
                "amount":"100.0000000000000000",
                "serialNum":"1000022344519109",
                "type":1,
                "time":1552369800000
            }
        ]
    },
    "success":true
}
```

 **返回参数说明** 

| 参数名    | 类型    | 说明                    |
| :-------- | :------ | ----------------------- |
| brokerId  | String  | 券商id                  |
| amount    | String  | 划转金额                |
| serialNum | String  | 流水号                  |
| userId    | String  | 用户id                  |
| assetId   | Integer | 币种id                  |
| assetName | String  | 币种名称                |
| type      | Integer | 划转类型:1 转入，2 转出，3 结算 |
| time      | Long    | 创建时间                |


### 完整交易记录

**简要描述：** 

- 完整的已结算的交易记录，带分页的

**请求URL：** 

- url + `v1/users/subaccount/order/page`

**请求方式：**

- GET

**参数：** 

| 参数名    | 必选 | 类型    | 说明                                                  |
| :-------- | :--- | :------ | ----------------------------------------------------- |
| userId    | 是   | String  | 用户id                                                |
| startTime | 否   | Long    | 下单时间（tradingTime）的开始时间，到毫秒             |
| endTime   | 否   | Long    | 下单时间（tradingTime）的结束时间，到毫秒             |
| pageNo    | 否   | Integer | 页码，默认1                                           |
| pageSize  | 否   | Integer | 每页条数，默认20，默认按照下单时间（tradingTime）倒序 |
| assetId   | 否   | Integer | 资产币种id，不传则返回所有的资产币种数据                |

**返回示例**

```
{
    "code" : 0,
    "data" : {
    	"pageNo": 1,
        "pageSize": 20,
        "total": 166,
        "item": [{
                "assetId": 2,
                "asset": "BTC",
                "underlying": "BTC/USD",
                "tradingTime": 1550722124000,
                "settlementTime": 1550722200000,
                "price": "0.0001",
                "profitAndLoss": 1
                "profit": "0.00003",
                "direction": 1,
                "rateOfReturn": "0.003",
                "spotIndex": "2345.22",
                "optionType": 1,
                "orderNum": "111111111"
         }]
    },
    "success": true
}
```

 **返回参数说明** 

| 参数名         | 类型    | 说明                                                         |
| :------------- | :------ | ------------------------------------------------------------ |
| assetId        | Integer | 资产币种id                                                       |
| asset          | String  | 资产币种名称                                                     |
| underlying     | String  | 标的
| tradingTime    | Long    | 下单时间                                                     |
| settlementTime | Long    | 结算时间                                                     |
| profitAndLoss  | Integer | 盈亏：1盈利，2亏损                                            |
| price          | String  | 订单金额                                                     |
| profit         | String  | 收益                                                         |
| direction      | Integer | 看涨看跌:1 看涨，2 看跌                                      |
| rateOfReturn   | String  | 收益率                                      |
| spotIndex      | String  | 下单时现货指数                                  |
| optionType     | Integer | 期权类型:1 ATM期权，2 OTM期权                                      |
| orderNum       | String  | 订单号                                    |

### 券商单个子账号余额查询

**简要描述：** 

- 券商子账号余额

**请求URL：** 

- url + `v1/users/subaccount/balance`

**请求方式：**

- GET

**参数：** 

| 参数名 | 必选 | 类型   | 说明   |
| :----- | :--- | :----- | ------ |
| userId | 是   | String | 用户id |

**返回示例**

```
{
    "msg": null,
    "code": 0,
    "data": [
        {
            "brokerId": "13",
            "assetId": 1,
            "assetName": "USDT",
            "amount": "11.0000000000000000"
        },
        {
            "brokerId": "13",
            "assetId": 2,
            "assetName": "BTC",
            "amount": "10000011.7966132400000000"
        },
        {
            "brokerId": "13",
            "assetId": 3,
            "assetName": "ETH",
            "amount": "9999999.8317700000000000"
        }
    ],
    "success": true
}
```

 **返回参数说明** 

| 参数名    | 类型    | 说明     |
| :-------- | :------ | -------- |
| brokerId  | String  | 券商id   |
| assetId   | Integer | 标的id   |
| assetName | String  | 标的名称 |
| amount    | String  | 余额     |

### 券商所有子账号余额查询

**简要描述：** 

- 券商所有子账号余额查询

**请求URL：** 

- url + `v1/users/subaccount/balance/page`

**请求方式：**

- GET

**参数：** 

| 参数名 | 必选 | 类型   | 说明   |
| :----- | :--- | :----- | ------ |
| assetId | 是   | Integer | 币种id |
| amountOrderBy | 否   | Integer | 用户余额排序规则，默认为1（asc），2为desc |
| pageNum    | 否   | Integer | 页码，默认1                                           |
| pageSize  | 否   | Integer | 每页条数，默认20 |

**返回示例**

```
{
    "msg": null,
    "code": 0,
    "data": {
        "pageNo": 1,
        "pageSize": 20,
        "total": 8,
        "item": [
            {
                "userId": "1547694206089",
                "amount": "20.0000000000000000"
            },
            {
                "userId": "1547640078864",
                "amount": "0.9395286200000000"
            },
            {
                "userId": "1547633327972",
                "amount": "0.9368608577000000"
            },
            {
                "userId": "1547639283857",
                "amount": "0.0000000000000000"
            },
            {
                "userId": "1547652167083",
                "amount": "0.0000000000000000"
            },
            {
                "userId": "1547694098426",
                "amount": "0.0000000000000000"
            },
            {
                "userId": "1547700411775",
                "amount": "0.0000000000000000"
            },
            {
                "userId": "1547714221718",
                "amount": "0.0000000000000000"
            }
        ]
    },
    "success": true
}
```

 **返回参数说明** 

| 参数名    | 类型    | 说明     |
| :-------- | :------ | -------- |
| userId  | String  | 用户id   |
| amount    | String  | 余额     |

### 券商支持币种查询
**简要描述：** 

- 券商支持币种查询

**请求URL：** 

- url + `v1/option/assets`

**请求方式：**

- GET

**参数：** 

| 参数名 | 必选 | 类型   | 说明   |
| :----- | :--- | :----- | ------ |
| brokerId | 否   | String | 券商id |

**返回示例**

```
{
  "code" : 0,
  "data" : {
    "assets" : [
      {
        "id" : 2,
        "name" : "BTC"
      },
      {
        "id" : 3,
        "name" : "ETH"
      },
      {
        "id" : 999,
        "name" : "VFOTA"
      }
    ]
  },
  "success" : true,
  "msg" : null
}
```

 **返回参数说明** 

| 参数名    | 类型    | 说明     |
| :-------- | :------ | -------- |
| id   | Integer | 标的id   |
| name | String  | 标的名称 |

### 券商子账号查询
**简要描述：** 

- 券商子账号查询，存在就返回账号信息

**请求URL：** 

- url + `v1/users/subaccount/account`

**请求方式：**

- GET

**参数：** 

| 参数名 | 必选 | 类型   | 说明   |
| :----- | :--- | :----- | ------ |
| suid | 是   | String | 第三方平台的用户id |

**返回示例**

```
{
  "code" : 0,
  "data" : {
    "uid" : "222222",
    "password" : "123456",
    "suid" : "15531"
  },
  "success": true
}
```

 **返回参数说明** 

| 参数名   | 类型   | 说明               |
| :------- | :----- | ------------------ |
| uid      | String | 用户id             |
| password | String | 为用户生成的密码   |
| suid     | String | 第三方平台的用户id |

### 公共错误码

| 错误代码 | 中文含义解释                       | 英文含义解释 | 说明 |
| :------- | :--------------------------------- | :----------- | ---- |
| 100000   | 错误的请求                         |              |      |
| 100001   | apikey有误                         |              |      |
| 100002   | 无效的签名                         |              |      |
| 100003   | 请求时间戳过期                     |              |      |
| 100004   | 请求太频繁                         |              |      |
| 100005   | 禁止访问                           |              |      |
| 100006   | 未找到请求的资源                   |              |      |
| 100007   | 使用的 HTTP 方法不适用于请求的资源 |              |      |
| 100008   | 请求的内容格式不是 JSON            |              |      |
| 100009   | 服务内部错误，请稍后再进行尝试     |              |      |
| 100010   | 服务不可用，请稍后再进行尝试       |              |      |
| 100011  | 同一个流水号只能使用一次       |              |      |
| 100012  | 相同的suid和brokerId只能注册一个账户       |              |      |
| 200000  | 登录失败，用户ID或密码不对       |              |      |
| 200001  | 该币种的资产余额不足       |              |      |
| 200002  | 用户不存在       |              |      |
| 200003  | 用户资产信息为空，请先转入资产 | | |
| 200004  | 用户资产里面没有该币种的资产信息，请先转入该币种资产 | | |
| 200005  | userId和brokerId不对应，请检查apikey是否有误 | | |
| 300000  | userId参数错误       |              |      |
| 300001  | amount参数错误       |              |      |
| 300002  | assetId参数错误       |              |      |
| 300003  | assetName参数错误       |              |      |
| 300004  | serialNum参数错误       |              |      |
| 300005  | suid参数错误       |              |      |
| 300006  | email参数错误       |              |      |
| 300007  | phone参数错误       |              |      |
| 300008  | name参数错误       |              |      |
| 300009  | email和phone必须要有一个       |              |      |
| 300011  | password参数错误       |              |      |
| 300012  | pageNum参数错误       |              |      |
| 300013  | pageSize参数错误 | | |
| 300014  | startTime参数错误 | | |
| 300015  | endTime参数错误 | | |
| 300016  | brokerId参数错误 | | |
| 300017  | assetId/assetName参数错误 | | |

# Android集成文档

GitHub地址：[optionDemo-Android](https://github.com/FotaPodRepo/optionDemo-Android)

# iOS集成文档

GitHub地址：[optionSDK-iOS](https://github.com/FotaPodRepo/optionSDK-iOS)

# Web集成文档
GitHub地址：[optionSDK-web](https://github.com/FotaPodRepo/optionSDK-web)
