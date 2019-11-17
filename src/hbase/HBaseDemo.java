package hbase;


import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import test.EntityToString;
import test.StringHandle;




/*
 * HBase文件和本地文件监听之HBase文件操作类 V1.00
 */



public class HBaseDemo {


	EntityToString ets=new EntityToString();
	//连接集群
	public Connection initHbase() throws IOException {

		Configuration configuration = HBaseConfiguration.create();
		configuration.set("hbase.zookeeper.property.clientPort", "2181");
		configuration.set("hbase.zookeeper.quorum", "192.168.57.128");
		//集群配置↓
		//configuration.set("hbase.zookeeper.quorum", "101.236.39.141,101.236.46.114,101.236.46.113");
		configuration.set("zookeeper.znode.parent","/hbase-unsecure");
		configuration.set("hbase.master", "192.168.57.128:5555");
		Connection connection = ConnectionFactory.createConnection(configuration);
		return connection;
	}

	private Admin admin;
	//创建表
	public void createTable(String tableNmae, String[] cols)  {

		TableName tableName = TableName.valueOf(tableNmae);
		try {
			admin = initHbase().getAdmin();
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		try {
			if (admin.tableExists(tableName)) {
				System.out.println("表已存在！");
			} else {
				HTableDescriptor hTableDescriptor = new HTableDescriptor(tableName);
				for (String col : cols) {
					HColumnDescriptor hColumnDescriptor = new HColumnDescriptor(col);
					hTableDescriptor.addFamily(hColumnDescriptor);
				}
				admin.createTable(hTableDescriptor);
			}
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
	}
	
	
	//获取原始数据
	public void getNoDealData(String tableName){
		try {
			Table table= initHbase().getTable(TableName.valueOf(tableName));
			Scan scan = new Scan();
			ResultScanner resutScanner = table.getScanner(scan);
			for(Result result: resutScanner){
				System.out.println("scan:  " + result);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	//插入数据
	public <T> void insertData(String tableName, T info,Map<String,String> infos) {
		TableName tablename = TableName.valueOf(tableName);
		Put put = new Put(Bytes.toBytes( ets.getNameValue(info, infos.get("key"))));

		List<String> nameString=ets.getNameList(info.getClass());
		for(int i=0;i<nameString.size();i++)
		{
			//参数：1.列族名  2.列名  3.值
			if(infos.get(nameString.get(i))!=null)
				put.addColumn(Bytes.toBytes(infos.get(nameString.get(i))), Bytes.toBytes(nameString.get(i)), Bytes.toBytes(ets.getNameValue(info, nameString.get(i)))) ;
		}
		//HTable table = new HTable(initHbase().getConfiguration(),tablename);已弃用
		Table table=null;
		try {
			table = initHbase().getTable(tablename);
			table.put(put);
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		
	}
	
	//插入数据
	public void insertData(String tableName,String row,String []name,String []value,Map<String,String> infos) {
		TableName tablename = TableName.valueOf(tableName);
		StringHandle sh=new StringHandle();
		Map<String,String> tp=sh.StringListToMap(sh.StringNlistToStringList(name), sh.StringNlistToStringList(value));
		Put put = new Put(Bytes.toBytes(row));

		for(int i=0;i<name.length;i++)
		{ 
			//参数：1.列族名  2.列名  3.值
			if(infos.get(name[i])!=null)
				put.addColumn(Bytes.toBytes(infos.get(name[i])), Bytes.toBytes(name[i]), Bytes.toBytes(tp.get(name[i]))) ;
		}
		//HTable table = new HTable(initHbase().getConfiguration(),tablename);已弃用
		Table table=null;
		try {
			table = initHbase().getTable(tablename);
			table.put(put);
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		
	}
	//以fields以类似Info:name的格式添加
	public void addRecord(String tableName, String row, String[] fields, String[] values)
	{
		StringHandle sh=new StringHandle();
		List<List<String>> infos=sh.StringSplitByExpToStringList(sh.StringNlistToStringList(fields), ":");
		List<String> info1=sh.StringListListInitSingleList(infos, 0);
		List<String> info2=sh.StringListListInitSingleList(infos, 1);
		Map<String,String> m=new HashMap<String,String>();
		for(int i=0;i<fields.length;i++)
		{
			m.put(info2.get(i), info1.get(i));
		}
		insertData(tableName, row,sh.StringListToStringNlist(info2), values,m);
	}
	
	/**
     * 是插入的一个函数，插入的是一个put的list。具体创建方法
     * List<Put> putList = new ArrayList<put>();
     * Put put;
     * for(int i = 0; i < 10; i++){
     *  put = new Put(Bytes.toBytes("row" + i));
     *  put.addColumn(Bytes.toBytes("Base")//列簇,Bytes.toBytes("name")//列名,Bytes.toBytes("bookName")//值);
     *  putList.ad(put);
     * }
     * 
     * @param tableNameString
     * @param putList
     * @throws IOException
     */
    public void insert(String tableNameString, List<Put> putList) {
        System.out.println("开始执行插入操作");

        //取得一个数据表对象
        Table table=null;
		try {
			table = initHbase().getTable(TableName.valueOf(tableNameString));
			//将数据插入到数据库中
	        table.put(putList);
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
        System.out.println("插入成功");
    }
    
    public void insert(String tableNameString, Put put) {
        System.out.println("开始执行插入操作");

        //取得一个数据表对象
        Table table=null;
		try {
			table = initHbase().getTable(TableName.valueOf(tableNameString));
			//将数据插入到数据库中
	        table.put(put);
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
        System.out.println("插入成功");
    }
    
    
    //将变量设置为Put类型
    public <T> Put makePut(T info,Map<String,String> infos,List<String> nameList)
    {

    	Put put = new Put(Bytes.toBytes(ets.getNameValue(info, infos.get("key"))));
    	for(int i=0;i<nameList.size();i++)
    	{
    		if(infos.get(nameList.get(i))!=null)
    		{
    			put.addColumn(Bytes.toBytes(infos.get(nameList.get(i))),Bytes.toBytes(nameList.get(i)),Bytes.toBytes(ets.getNameValue(info, nameList.get(i))));
    		}
    	}
    	return put;
    }
    
    //将变量设置为Put类型
    public <T> Put makePut(T info,Map<String,String> infos)
    {
    	List<String> nameList=ets.getNameList(info.getClass());
    	Put put = new Put(Bytes.toBytes(ets.getNameValue(info, infos.get("key"))));
    	for(int i=0;i<nameList.size();i++)
    	{
    		if(infos.get(nameList.get(i))!=null)
    		{
    			put.addColumn(Bytes.toBytes(infos.get(nameList.get(i))),Bytes.toBytes(nameList.get(i)),Bytes.toBytes(ets.getNameValue(info, nameList.get(i))));
    		}
    	}
    	return put;
    }
    
    //将对应值设置为Put类型
    //public Put makePut(String )

	//将变量容器设置为Put容器
    public <T> List<Put> makePutList(List<T> infoList,Map<String,String> infos)
    {
    	List<Put> plist=new ArrayList<Put>();
    	if(infoList.size()==0)
    		return plist;
    	List<String> nameList=ets.getNameList(infoList.get(0).getClass());
    	int g_num=infoList.size();
    	for(int i=0;i<g_num;i++)
    	{
    		plist.add(makePut(infoList.get(i),infos,nameList));
    	}
    	return plist;
    }
	

	//删除指定cell数据
	public void deleteByRowKey(String tableName, String rowKey) {

		Table table=null;
		try {
			table = initHbase().getTable(TableName.valueOf(tableName));
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		Delete delete = new Delete(Bytes.toBytes(rowKey));
		//删除指定列
		//delete.addColumns(Bytes.toBytes("contact"), Bytes.toBytes("email"));
		try {
			table.delete(delete);
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
	}

	//删除表
	public void deleteTable(String tableName){

		try {
			TableName tablename = TableName.valueOf(tableName);
			admin = initHbase().getAdmin();
			admin.disableTable(tablename);
			admin.deleteTable(tablename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//根据rowKey进行查询
	public <T> T getDataByRowKey(String tableName, String rowKey,Class<T> it) throws IOException {

		Table table = initHbase().getTable(TableName.valueOf(tableName));
		Get get = new Get(Bytes.toBytes(rowKey));
		T bean=null;
		List<String> nameList=ets.getNameList(it);
		//先判断是否有此条数据
		if(!get.isCheckExistenceOnly()){
			Result result = table.get(get);
			try {
				bean=(T) it.newInstance();
			} catch (InstantiationException | IllegalAccessException e1) {
				// TODO 自动生成的 catch 块
				e1.printStackTrace();
			}
			for (Cell cell : result.rawCells()){
				String row = Bytes.toString(cell.getRowArray(), cell.getRowOffset(), cell.getRowLength());
				String colName = Bytes.toString(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength());
				String value = Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
				
				
				// 循环遍历字段，获取字段相应的属性值

				try
				{
					nameList.remove(colName);
					Field fs=getDeclaredField(bean, colName);
					if(fs==null){
						throw new IllegalArgumentException("Could not find field["+ 
								colName+"] on target ["+bean+"]");
					}
					makeAccessiable(fs);
					try{
						fs.set(bean, (Object)value);
					}
					catch(IllegalAccessException e){
						System.out.println("不可能抛出的异常");
					}
					if(nameList.size()==1)
					{
						fs=getDeclaredField(bean, nameList.get(0));
						if(fs==null){
							throw new IllegalArgumentException("Could not find field["+ 
									nameList.get(0)+"] on target ["+bean+"]");
						}
						makeAccessiable(fs);
						try{
							fs.set(bean, (Object)row);
						}
						catch(IllegalAccessException e){
							System.out.println("不可能抛出的异常");
						}
					}
					// 设置字段可见，就可以用get方法获取属性值。
					//result += field.get( o ) +" ";
				}
				catch ( Exception e )
				{
					// System.out.println("error--------"+methodName+".Reason is:"+e.getMessage());
				}

			}
			
			
		}
		return bean;
	}
	
	//获得一个列表中所有的rowKey
	public List<String> getOnlyAllRowName(String tableName)
	{
		Table table=null;
		try {
			table = initHbase().getTable(TableName.valueOf(tableName));
		} catch (IOException e1) {
			// TODO 自动生成的 catch 块
			e1.printStackTrace();
		}
		Scan scan = new Scan();
		ResultScanner scan1;
		List<String> result=new ArrayList<String>();
		try {
			scan1 = table.getScanner(scan);
			
			for (Result r: scan1) { 
			
				for(Cell cell:r.rawCells())
				{
					String infos=Bytes.toString(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength());
					if(!result.contains(infos))
						result.add(infos);
				}
			};
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		return result;
		
	}
	
	
	
	//根据rowKey进行查询
	public List<String> getDataByRowKey(String tableName, String rowKey)  {

		Table table=null;
		try {
			table = initHbase().getTable(TableName.valueOf(tableName));
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		Get get = new Get(Bytes.toBytes(rowKey));
		List<String> ls=new ArrayList<String>();

		//先判断是否有此条数据
		if(!get.isCheckExistenceOnly()){
			Result result=null;
			try {
				result = table.get(get);
			} catch (IOException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
			String row=null;
			boolean g_throught=false;
			for (Cell cell : result.rawCells()){
				if(!g_throught)
				{
					row= Bytes.toString(cell.getRowArray(), cell.getRowOffset(), cell.getRowLength());
					g_throught=true;
				}
				//String colName = Bytes.toString(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength());
				String value = Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());


				// 循环遍历字段，获取字段相应的属性值
				ls.add(value);
			}
			ls.add(0,row);
		}
		return ls;
	}
	
	//得到某一列的列族及其限定符信息
	public List<String> getRowNameByRow(String tableName,String rowKey)
	{
		Table table=null;
		try {
			table = initHbase().getTable(TableName.valueOf(tableName));
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		Get get = new Get(Bytes.toBytes(rowKey));
		List<String> ls=new ArrayList<String>();

		//先判断是否有此条数据
		if(!get.isCheckExistenceOnly()){
			Result result=null;
			try {
				result = table.get(get);
			} catch (IOException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}

			for (Cell cell : result.rawCells()){

				String family =  Bytes.toString(cell.getFamilyArray(),cell.getFamilyOffset(),cell.getFamilyLength());
				String colName = Bytes.toString(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength());

				// 循环遍历字段，获取字段相应的属性值
				ls.add(family+":"+colName);
			}
		}
		return ls;
	}
		
	//添加指定列到对应数组上
	public void modifyData(String tableName, String row, String column,String value)
	{
		HBaseDemo hbd=new HBaseDemo();
		StringHandle sh=new StringHandle();
		List<String> a=hbd.getRowNameByRow(tableName, row);
		a.add(column);
		List<String> info=hbd.getDataByRowKey(tableName, row);
		info.remove(0);
		info.add(value);
		hbd.addRecord(tableName, row, sh.StringListToStringNlist(a), sh.StringListToStringNlist(info));
	}
	
	//查询指定单cell内容
	public String getCellData(String tableName, String rowKey, String family, String col){

		try {
			Table table = initHbase().getTable(TableName.valueOf(tableName));
			Get get = new Get(Bytes.toBytes(rowKey));
			if(!get.isCheckExistenceOnly()){
				get.addColumn(Bytes.toBytes(family),Bytes.toBytes(col));
				Result res = table.get(get);
				byte[] resByte = res.getValue(Bytes.toBytes(family), Bytes.toBytes(col));
				return Bytes.toString(resByte);
			}else{
				return "查询结果不存在";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "出现异常";
	}
	
	//添加列族
	public void addColumnFamily(String table,String familyname)
	{
		System.out.println("新建列簇开始");

        //取得目标数据表的标明对象
        TableName tableName = TableName.valueOf(table);

        //创建列簇对象
        HColumnDescriptor columnDescriptor = new HColumnDescriptor(familyname);

        //将新建的加入到指定的数据表
        try {
			admin=initHbase().getAdmin();
			admin.addColumn(tableName, columnDescriptor);
		} catch (IOException e1) {
			// TODO 自动生成的 catch 块
			e1.printStackTrace();
		}
        

        System.out.println("新建列簇结束");

		
	}
	
	 /**
     * 删除列簇的函数，第一个是表名，第二个是列簇名
     * @param tableNameString
     * @param columnFamily
     * @throws IOException
     */
    public void deleteColumnFamily(String tableNameString, String columnFamily) {
        System.out.println("删除列簇开始");
        //取得目标数据表的表明对象
        try {
			admin=initHbase().getAdmin();
			TableName tableName = TableName.valueOf(tableNameString);
	        //删除指定数据表中的指定列簇
	        admin.deleteColumn(tableName, Bytes.toBytes(columnFamily));
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
        System.out.println("删除列簇成功");
    }
	
	
	
	//查询指定表名中所有的数据
	public <T> List<T> getAllData(String tableName,Class<T> c){

		Table table = null;
		List<T> list = new ArrayList<T>();
		try {
			
			table = initHbase().getTable(TableName.valueOf(tableName));
			ResultScanner results = table.getScanner(new Scan());
			T bean=null;
			for (Result result : results){
				List<String> nameList=ets.getNameList(c);
				//String id = new String(result.getRow());
				//System.out.println("用户名:" + new String(result.getRow()));
				try {
					bean=(T) c.newInstance();
				} catch (InstantiationException | IllegalAccessException e1) {
					// TODO 自动生成的 catch 块
					e1.printStackTrace();
				}
				String row=null;
				boolean g_throught=false;
				for (Cell cell : result.rawCells()){
					if(!g_throught)
					{
						row= Bytes.toString(cell.getRowArray(), cell.getRowOffset(), cell.getRowLength());
						g_throught=true;
					}
					//String family =  Bytes.toString(cell.getFamilyArray(),cell.getFamilyOffset(),cell.getFamilyLength());
					String colName = Bytes.toString(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength());
					String value = Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
					
					try
					{
						nameList.remove(colName);
						Field fs=getDeclaredField(bean, colName);
						if(fs==null){
							throw new IllegalArgumentException("Could not find field["+ 
									colName+"] on target ["+bean+"]");
						}
						makeAccessiable(fs);
						try{
							fs.set(bean, (Object)value);
						}
						catch(IllegalAccessException e){
							System.out.println("不可能抛出的异常");
						}
						if(nameList.size()==1)
						{
							fs=getDeclaredField(bean, nameList.get(0));
							if(fs==null){
								throw new IllegalArgumentException("Could not find field["+ 
										nameList.get(0)+"] on target ["+bean+"]");
							}
							makeAccessiable(fs);
							try{
								fs.set(bean, (Object)row);
							}
							catch(IllegalAccessException e){
								System.out.println("不可能抛出的异常");
							}
						}
						// 设置字段可见，就可以用get方法获取属性值。
						//result += field.get( o ) +" ";
					}
					catch ( Exception e )
					{
						// System.out.println("error--------"+methodName+".Reason is:"+e.getMessage());
					}
				}
				list.add(bean);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}
	
	//获得hbase中表格的信息
	
	
	
	//获得表格中的所有数据
	public List<List<String>> getAllData(String tableName){

		Table table = null;
		List<List<String>> list = new ArrayList<List<String>>();
		try {
			
			table = initHbase().getTable(TableName.valueOf(tableName));
			ResultScanner results = table.getScanner(new Scan());
			for (Result result : results){
				List<String> tempInit=new ArrayList<String>();
				String row=null;
				boolean g_throught=false;
				for (Cell cell : result.rawCells()){
					if(!g_throught)
					{
						row= Bytes.toString(cell.getRowArray(), cell.getRowOffset(), cell.getRowLength());
						g_throught=true;
					}
					String value = Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
					tempInit.add(value);
					
				}
				tempInit.add(0, row);
				list.add(tempInit);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}
	
	
	
	
	//获得表中列的名字
	public List<String> getColName(String tableName)
	{
		List<String> ls=new ArrayList<String>();
		try {
			Table table = initHbase().getTable(TableName.valueOf(tableName));
			HTableDescriptor ht=table.getTableDescriptor();
			for(HColumnDescriptor fd:ht.getColumnFamilies())
			{
				ls.add(fd.getNameAsString());
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ls;
	}
	
	
	//清空某张表
	public void truncateTable(String tableNameString) {
		 
        try {
            admin = initHbase().getAdmin();
            System.out.println("开始清空数据");
 
            //取得目标数据表的表明对象
            TableName tableName = TableName.valueOf(tableNameString);
            //设置表状态为无效
            admin.disableTable(tableName);
            //清空指定表的数据
            admin.truncateTable(tableName, true);
            System.out.println("结束");
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
 
	//获得所有表的名称
	public List<String> getHbaseTableName()
	{
		List<String> ls=new ArrayList<String>();
		try {
			TableName []tbn=initHbase().getAdmin().listTableNames();
			for(TableName tbTemp:tbn)
			{
				ls.add(tbTemp.getNameAsString());
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ls;
	}
	


	//获取field属性，属性有可能在父类中继承 
	public Field getDeclaredField(Object obj,String fieldName){
		for (Class<?> clazz=obj.getClass(); clazz!=Object.class; clazz=clazz.getSuperclass()){
			try{
				return clazz.getDeclaredField(fieldName);
			}
			catch(Exception e){
			}
		}
		return null;
	}



	//判断field的修饰符是否是public,并据此改变field的访问权限 
	public void makeAccessiable(Field field){
		if(!Modifier.isPublic(field.getModifiers())){
			field.setAccessible(true);
		}
	}
//	 public static void main(String[] args){
//		 HBaseDemo hbd=new HBaseDemo();
//		 //信息对应表
//		 Map<String,String> infos=new HashMap<String,String>();
//		 infos.put("doemnumber", "dorm");
//		 infos.put("key", "number");
//		 infos.put("sex", "info");
//		 infos.put("name", "info");
//	        try {
//	        	//创建表
//	              hbd.createTable("student", new String[] {"info","dorm"});
//	        	
//	        	//插入值
//	        	Student per=new Student();
//	        	per.setDoemnumber("11");
//	        	per.setName("21");
//	        	per.setNumber("31");
//	        	per.setSex("41");
//	        	hbd.insertData("student", per, infos);
//	        	
//	        	//获得值
//	        	StringHandle sh=new StringHandle();
//	        	Student s=hbd.getDataByRowKey("student", "31", new Student().getClass());
//	        	System.out.println(sh.TToStringList(s));
//	        	
//	        	
//	        	//获得所有值
//	        	System.out.println(sh.TListToStringListList(hbd.getAllData("student", new Student().getClass())));
//	        	
//	        	//删除值
//	        	hbd.deleteByRowKey("student", "number-3");
//	        	System.out.println(sh.TListToStringListList(hbd.getAllData("student", new Student().getClass())));
//	        	
//	        	
//	        	
//	        } catch (Exception e) {
//	            e.printStackTrace();
//	        }
//	    }

}