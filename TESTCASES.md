# Задание 2.1. Тест-кейсы API

Стенд: `https://qa-internship.avito.com`. Все кейсы автоматизированы: класс и метод указаны в последней колонке, тесты лежат в `src/test/java/ru/avito/internship/tests`. Привязка кейса к тесту - в колонке «Автотест»: класс и метод.

Предусловия, общие для всех кейсов: пользователь зарегистрирован и авторизован, если в шагах не сказано иное. Тестовые данные генерируются на ход прогона, созданные объявления удаляются после теста.

Кейсы, помеченные «дефект», воспроизводят найденный баг: тест ожидает корректное поведение и падает ожидаемо. Номера дефектов - в [BUGS.md](BUGS.md).

## Покрытие и техники

| Техника | Где применена |
|---|---|
| Классы эквивалентности и граничные значения | длины username и пароля (3/64, 8), цена (0, 1, отрицательная, максимум), длина названия (1/300), id (UUID, не-UUID, верхний регистр, отсутствующий) |
| Таблица решений | матрица доступа: 6 защищённых ручек × 4 варианта авторизации (без токена, битый, чужая схема, пустой) |
| Переходы состояний | создание - чтение - статистика - удаление - повторное чтение, повторное удаление |
| Попарные сочетания | комбинации обязательных полей при создании объявления, частичная статистика |
| Негативные проверки | неверные типы, отсутствующие поля, чужие данные, подмена пользователя |
| Контрактные проверки | JSON-схемы ответов, формат createdAt, единый формат ошибок, коды ответов |
| Нефункциональные проверки | время ответа, Content-Type, идемпотентность чтения, параллельные запросы |

## Регистрация

| ID | Проверка | Шаги | Ожидаемый результат | Автотест |
|---|---|---|---|---|
| TC-REG-01 | Успешная регистрация | POST /api/1/register с новым username и паролем | 201, тело `{id, username}`, Content-Type JSON | `RegisterTest.newUser` |
| TC-REG-02 | Занятый username | POST с уже зарегистрированным username | 409, сообщение о занятом имени | `RegisterTest.duplicateUsername` |
| TC-REG-03 | Разные пользователи - разные id | Две регистрации с разными username | 201 и 201, id различаются | `RegisterTest.uniqueIds` |
| TC-REG-04 | Короткий username | username: `""`, `ab`, `a` | 400 | `RegisterTest.shortUsername` |
| TC-REG-05 | Длинный username | username из 200 символов | 400 | `RegisterTest.longUsername` |
| TC-REG-06 | Короткий пароль | пароль: `""`, `1`, `1234567` | 400 | `RegisterTest.shortPassword` |
| TC-REG-07 | Нет обязательных полей | тело без username, без password, `{}` | 400 | `RegisterTest.missingFields` |
| TC-REG-08 | Неверный тип username | username: `12345` | 400 | `RegisterTest.wrongType` |
| TC-REG-09 | Лишние поля | тело с `role: admin` | 201, лишнее поле игнорируется | `RegisterTest.extraFields` |
| TC-REG-10 | Битый JSON | тело `{not a json` | 400, Content-Type JSON | `RegisterTest.brokenJson` |
| TC-REG-11 | username из пробелов | username: `"   "` | 400 | `RegisterTest.blankUsername` |
| TC-REG-12 | Понятность ошибки длины username | username: `ab` | 400, сообщение объясняет длину, а не «invalid JSON body» (дефект API-11) | `RegisterTest.shortUsernameMessage` |
| TC-REG-13 | Неподдерживаемый метод | GET /api/1/register | 405 (дефект API-08: приходит 401) | `RegisterTest.methodNotAllowed` |
| TC-REG-14 | Границы длины username | username ровно 3 и ровно 64 символа | 201 в обоих случаях | `RegisterTest.usernameLengthBounds` |

## Авторизация

| ID | Проверка | Шаги | Ожидаемый результат | Автотест |
|---|---|---|---|---|
| TC-AUTH-01 | Успешная авторизация | POST /api/1/autorize с валидными кредами | 200, непустой accessToken, tokenType = Bearer | `AuthorizeTest.successfulAuthorization` |
| TC-AUTH-02 | Неверный пароль | пароль не совпадает | 401 | `AuthorizeTest.wrongPassword` |
| TC-AUTH-03 | Неизвестный пользователь | username не существует | 401 | `AuthorizeTest.unknownUser` |
| TC-AUTH-04 | Нет пароля | тело без password и `{}` | 400 | `AuthorizeTest.missingPassword` |
| TC-AUTH-05 | Короткий пароль | `""`, `1`, `1234567` | 400 | `AuthorizeTest.shortPassword` |
| TC-AUTH-06 | Регистр username | username в верхнем регистре | 401 | `AuthorizeTest.caseSensitiveUsername` |
| TC-AUTH-07 | Токен открывает защищённую ручку | GET своего объявления с полученным токеном | 200 | `AuthorizeTest.tokenOpensEndpoint` |
| TC-AUTH-08 | Понятность ошибки длины пароля | пароль `123` | 400 с сообщением про длину (дефект API-11) | `AuthorizeTest.shortPasswordMessage` |
| TC-AUTH-09 | Ответ не раскрывает пароль | POST /api/1/autorize | в теле только accessToken и tokenType | `AuthorizeTest.noPasswordInResponse` |

## Доступ к защищённым ручкам

Набор ручек: GET /api/1/item/{id}, GET /api/1/{sellerID}/item, GET /api/1/statistic/{id}, GET /api/2/statistic/{id}, POST /api/1/item, DELETE /api/2/item/{id}.

| ID | Проверка | Шаги | Ожидаемый результат | Автотест |
|---|---|---|---|---|
| TC-SEC-01 | Без заголовка Authorization | каждый из 6 эндпоинтов без токена | 401 | `AccessTest.noToken` |
| TC-SEC-02 | Битый токен | `Bearer garbage.token.value` | 401 | `AccessTest.garbageToken` |
| TC-SEC-03 | Чужая схема авторизации | `Basic dXNlcjpwYXNz` | 401 | `AccessTest.otherAuthScheme` |
| TC-SEC-04 | Пустой Bearer | `Authorization: Bearer` | 401 | `AccessTest.emptyBearer` |
| TC-SEC-05 | Подмена продавца | создание с чужим sellerID и заголовком X-UserId | 400, объявление не создаётся | `AccessTest.xUserIdCannotSpoof` |
| TC-SEC-06 | Валидный токен | GET несуществующего объявления | любой код, кроме 401 | `AccessTest.validToken` |

## Создание объявления

| ID | Проверка | Шаги | Ожидаемый результат | Автотест |
|---|---|---|---|---|
| TC-ITEM-01 | Успешное создание | POST /api/1/item с валидным телом | 200, в status сообщение и UUID | `CreateItemTest.createReturnsUuid` |
| TC-ITEM-02 | Поля сохраняются | создать, затем GET по id | name, price, sellerId, statistics совпадают | `CreateItemTest.itemFieldsRoundTrip` |
| TC-ITEM-03 | Чужой sellerID | sellerID другого пользователя | 400, сообщение про sellerID | `CreateItemTest.foreignSellerId` |
| TC-ITEM-04 | Обязательные поля | поочерёдно убрать sellerID, name, price, statistics | 400 в каждом случае | `CreateItemTest.requiredFields` |
| TC-ITEM-05 | Нулевая цена | price: 0 | 200, ноль - валидное число (дефект API-04: 400) | `CreateItemTest.zeroPrice` |
| TC-ITEM-06 | Отрицательная цена | price: -500 | 400 (дефект API-03: 200) | `CreateItemTest.negativePrice` |
| TC-ITEM-07 | Допустимые цены | 1, 999, 100000, 2147483647 | 200 для каждого значения | `CreateItemTest.priceBounds` |
| TC-ITEM-08 | Тип price | price: 10.5 | 400 с сообщением про тип поля (дефект API-06) | `CreateItemTest.fractionalPriceMessage` |
| TC-ITEM-09 | Нулевые счётчики статистики | likes, viewCount, contacts = 0 | 200 (дефект API-04: 400) | `CreateItemTest.zeroStatistics` |
| TC-ITEM-10 | Отрицательный счётчик | likes: -1 | 400 (дефект API-03: 200) | `CreateItemTest.negativeStatistics` |
| TC-ITEM-11 | Неполная статистика | нет поля contacts | 400, сообщение называет contacts | `CreateItemTest.incompleteStatistics` |
| TC-ITEM-12 | Лишний ключ статистики | добавлен shares: 5 | 200, ключ игнорируется | `CreateItemTest.extraStatisticsKey` |
| TC-ITEM-13 | Длина названия | name из 1, 300 символов и пустая строка | 200, 200, 400 | `CreateItemTest.nameLengths`, `CreateItemTest.emptyName` |
| TC-ITEM-14 | Название из пробелов | name: `"    "` | 400 (дефект API-10: 200) | `CreateItemTest.blankName` |
| TC-ITEM-15 | Лишние поля тела | добавлено поле description | 200, поле игнорируется | `CreateItemTest.extraFields` |
| TC-ITEM-16 | Параллельное создание | 5 одновременных запросов | 5 разных UUID | `CreateItemTest.parallelCreation` |

## Получение объявления

| ID | Проверка | Шаги | Ожидаемый результат | Автотест |
|---|---|---|---|---|
| TC-GET-01 | Объявление по id | GET /api/1/item/{id} | 200, массив из одного элемента, id совпадает | `GetItemTest.singleElementArray` |
| TC-GET-02 | Схема ответа | GET объявления | соответствует `schemas/item.json` | `GetItemTest.matchesSchema` |
| TC-GET-03 | Несуществующий id | случайный UUID | 404, в сообщении есть id | `GetItemTest.unknownItem` |
| TC-GET-04 | Не-UUID | id = `123` | 400, сообщение про UUID | `GetItemTest.malformedId` |
| TC-GET-05 | UUID в верхнем регистре | id в верхнем регистре | 200 | `GetItemTest.uppercaseUuid` |
| TC-GET-06 | Формат createdAt | GET объявления | дата разбирается как ISO-8601 с одним смещением (дефект API-07) | `GetItemTest.createdAtFormat` |

## Объявления продавца

| ID | Проверка | Шаги | Ожидаемый результат | Автотест |
|---|---|---|---|---|
| TC-SELLER-01 | Свои объявления | создать два, GET /api/1/{sellerID}/item | 200, оба id в списке | `SellerItemsTest.ownItems` |
| TC-SELLER-02 | Продавец без объявлений | GET для несуществующего sellerID | 200, пустой массив | `SellerItemsTest.emptyList` |
| TC-SELLER-03 | Нечисловой sellerID | GET /api/1/abc/item | 400 | `SellerItemsTest.nonNumericSellerId` |
| TC-SELLER-04 | Набор полей | GET списка | у элемента есть createdAt, id, name, price, sellerId, statistics | `SellerItemsTest.itemFields` |

## Статистика

| ID | Проверка | Шаги | Ожидаемый результат | Автотест |
|---|---|---|---|---|
| TC-STAT-01 | Версия 1 | GET /api/1/statistic/{id} | 200, счётчики как при создании, схема валидна | `StatisticTest.v1Counters` |
| TC-STAT-02 | Версия 2 | GET /api/2/statistic/{id} | 200, счётчики как при создании | `StatisticTest.v2Counters` |
| TC-STAT-03 | Версии совпадают | оба запроса для одного объявления | одинаковое тело | `StatisticTest.versionsMatch` |
| TC-STAT-04 | Несуществующий id | обе версии на случайный UUID | 404 | `StatisticTest.unknownItem` |
| TC-STAT-05 | Не-UUID | id = `abc` в версии 1 | 400 | `StatisticTest.malformedId` |
| TC-STAT-06 | После удаления | удалить объявление, запросить статистику | 404 (дефект API-02: 200 со старыми данными) | `StatisticTest.afterDeletion` |
| TC-STAT-07 | Одинаковые коды для не-UUID | id = `abc` в обеих версиях | одинаковый HTTP-код (дефект API-09: 400 и 404) | `StatisticTest.sameCodeForMalformedId` |

## Удаление объявления

| ID | Проверка | Шаги | Ожидаемый результат | Автотест |
|---|---|---|---|---|
| TC-DEL-01 | Удаление владельцем | DELETE /api/2/item/{id} | 200, пустое тело, последующий GET даёт 404 | `DeleteItemTest.ownerDeletes` |
| TC-DEL-02 | Чужое объявление | DELETE с токеном другого пользователя | 403 или 404, объявление остаётся доступным (дефект API-01: 200, объявление удаляется) | `DeleteItemTest.foreignItem` |
| TC-DEL-03 | Повторное удаление | удалить дважды | 404, в теле `status: "404"` (дефект API-05: в теле 500) | `DeleteItemTest.repeatedDeleteStatus` |
| TC-DEL-04 | Несуществующее объявление | DELETE случайного UUID | 404, в теле `status: "404"` (дефект API-05) | `DeleteItemTest.unknownDeleteStatus` |
| TC-DEL-05 | Не-UUID | DELETE /api/2/item/abc | 400 | `DeleteItemTest.malformedId` |

## Контракт ответов

| ID | Проверка | Шаги | Ожидаемый результат | Автотест |
|---|---|---|---|---|
| TC-CON-01 | status в теле равен HTTP-коду | создание с price 10.5 | в теле `status: "400"` (дефект API-06: там текст) | `ContractTest.bodyStatusIsCode` |
| TC-CON-02 | Ошибки отдаются как JSON | 401 и 400 | Content-Type application/json | `ContractTest.errorsAreJson` |

## Сквозной сценарий

| ID | Проверка | Шаги | Ожидаемый результат | Автотест |
|---|---|---|---|---|
| TC-E2E-01 | Полный жизненный цикл | регистрация, авторизация, создание, чтение, статистика v1 и v2, удаление, повторное чтение | каждый шаг с ожидаемым кодом, после удаления 404 | `EndToEndTest.fullItemLifecycle` |

## Нефункциональные проверки

| ID | Проверка | Шаги | Ожидаемый результат | Автотест |
|---|---|---|---|---|
| TC-NF-01 | Время ответа | GET объявления | меньше 3000 мс | `NonFunctionalTest.responseTime` |
| TC-NF-02 | Content-Type | GET объявления, статистики, списка продавца | application/json | `NonFunctionalTest.contentType` |
| TC-NF-03 | Идемпотентность чтения | два GET подряд | тела совпадают | `NonFunctionalTest.repeatedRead` |
| TC-NF-04 | Параллельное чтение | 10 одновременных GET одного объявления | все ответы 200 | `NonFunctionalTest.parallelReads` |

## Итого

74 кейса, 108 автоматических тестов (часть кейсов проверяется несколькими наборами данных: границы длин, границы цены, матрица доступа). 16 тестов помечены `@KnownBug` и в прогоне видны как пропущенные с номером дефекта.
