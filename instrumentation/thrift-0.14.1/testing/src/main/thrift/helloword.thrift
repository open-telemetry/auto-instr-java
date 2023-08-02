namespace java thrifttest

// 服务名

struct Account {
    1:required string zone;
    2:required string cardId;
}

struct User {
    1:required string name;
    2:required string userId;
    3:required i32 age;
}

struct userAccount{
    1:required User user;
    2:required Account account;
}

service HelloWorldService {
    string sayHello(1:string zone,2:string name);
    string withDelay(1:i32 delay);
    string withoutArgs();
    string withError();
    string withCollisioin(3333:string input);
    oneway void oneWay();
    oneway void oneWayWithError();
    userAccount data(1:User user,2:Account account);
}

