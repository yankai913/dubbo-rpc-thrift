namespace java com.sodao.demo.services.t1

struct User
{
  1: i32 id,
  2: string name,
  3: i32 age
}

exception Xception {
  1: i32 errorCode,
  2: string message
}

/** 异常名称err1**/
service HelloService
{
	User getUser(1: i32 id, 2: string name, 3: i32 age) throws (1:Xception err1);
	string getString(1:string str);
	void sayHello(1:string str);
}
