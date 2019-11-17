package hbase;

public class hbase {
	public static void main(String arg[])
	{
		HBaseDemo hbd=new HBaseDemo();
		hbd.createTable("ook", new String[] {"test"});
	}
}
