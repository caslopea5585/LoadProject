package oracle;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;

import util.file.FileUtil;

public class LoadMain extends JFrame implements ActionListener,TableModelListener{
	JPanel p_north;
	JTextField t_path;
	JButton bt_open, bt_load,bt_excel,bt_del;
	JTable table;
	JScrollPane scroll;
	JFileChooser chooser;
	FileReader reader= null;
	BufferedReader buffr = null;
	Connection con; //������ â�� ������ �̹� ������ Ȯ���س���
	DBManager manager=DBManager.getInstance();
	ArrayList<String> value= new ArrayList<String>();
	Vector<Vector> list;
	Vector ColumnName;
	MyModel myModel;
	
	public LoadMain() {
		p_north = new JPanel();
		t_path=new JTextField(25);
		bt_open=new JButton("CSV���Ͽ���");
		bt_load=new JButton("�ε��ϱ�");
		bt_excel= new JButton("�����ε�");
		bt_del=new JButton("�����ϱ�");
		table = new JTable();
		scroll = new JScrollPane(table);
		chooser = new JFileChooser("C:/animal");
		
		p_north.add(t_path);
		p_north.add(bt_open);
		p_north.add(bt_load);
		p_north.add(bt_excel);
		p_north.add(bt_del);
		
		add(p_north, BorderLayout.NORTH);
		add(scroll);
		
		bt_open.addActionListener(this);
		bt_load.addActionListener(this);
		bt_excel.addActionListener(this);
		bt_del.addActionListener(this);
		
		
		
		//������� �������� ����
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				manager.disConnect(con); 	//�����ͺ��̽� �ڿ� ����
				System.exit(0); 				//���μ��� ����
			
			}
		});
		
		setVisible(true);
		setSize(800, 600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		init();
		
	}
	
	//���� Ž���� ����
	public void open(){
		int result = chooser.showOpenDialog(this);
		//���⸦ ������ ���������� ��Ʈ���� ��������.
		if(result==JFileChooser.APPROVE_OPTION){
			File file = chooser.getSelectedFile(); //������ ������ ����
			String ext = FileUtil.getExt(file.getName());
			
			if(!ext.equals("csv")){
				JOptionPane.showMessageDialog(this, "CSV�� �־��ּ���!");
				return;//���ื��
			}else{
				t_path.setText(file.getAbsolutePath());
				try {
					reader =new FileReader(file);
					buffr = new BufferedReader(reader);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}			
			}
		}
	}
	
	//CSV -> Oracle������ ����(migration)�ϱ�
	public void load(){
		//���� ��Ʈ���� �̿��Ͽ� csv�� �����͸� ���پ� �о�鿩 insert��Ű��.
		//���ڵ尡 ����������...while������ ������ �ʹ� �����Ƿ�...��Ʈ���� ���� �Ұ�
		//�Ϻη�...������Ű�鼭 ������ (�ӵ���ũ�� ���� ������ �����Ͱ� ���� �� �� ����)
		String data;
		StringBuffer sb = new StringBuffer();
		PreparedStatement pstmt=null;
		
		try {
			
			while(true){
				data = buffr.readLine();	
				if(data==null)break;
				String[] value = data.split(",");
				if(!value[0].equals("seq")){ //�߰ߵ��� ������쿡�� insert�Ҳ���~
					sb.append("insert into hospital(seq,name,addr,regdate,status,dimension,type)");
					sb.append(" values("+value[0]+",'"+value[1]+"','"+value[2]+"','"+value[3]+"','"+value[4]+"',"+value[5]+",'"+value[6]+"')");
					
					pstmt=con.prepareStatement(sb.toString());
					int result = pstmt.executeUpdate(); //��������
					System.out.println(sb.toString());
					//������ ������ StringBuffer�� �����͸� ��������
					sb.delete(0, sb.length());
					
				}else{
					System.out.println("������");
				}
			}
			JOptionPane.showMessageDialog(this, "���̱׷��̼� �Ϸ�!!");
			
			//JTable������ ó��!!
			getList();
			table.setModel(new MyModel(list, ColumnName));
			table.getModel().addTableModelListener(this);//���̺� �𵨰� �����ʿ��� ����(���̺� �𵨰� ������ �����̹Ƿ�..���̺��� ���� �ִ� ���� �޾ƿͼ� �ű⿡ �����ʸ� �ٿ�����)
			//���� Mymodel�� �������� ���� �ص��Ǳ���...�� �̷���� �������� ���̰� �߻��� �� �� ����.
			table.updateUI();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
			try {
				if(pstmt!=null){
					pstmt.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
/*	//�������� �о db�� ���̱׷��̼� �ϱ�...
	//javaSE �������� ���̺귯�� �ִ�??? X
	//openSource  ��������Ʈ����
	//copyright(����Ʈ�ǹ���ȭ) <----> copyleft(����Ʈȭ����ȭ / ����ġ��ü)
	//POI ���̺귯��!! http://apache.org
	 * (�������� �����ϱ� ���� ���� ����)
	 * HSSFWorkBook : ��������
	 * HSSFSheet : ��Ʈ(sheet)
	 * HSSFRow : �� ��...row
	 * HSSFCell : cell 
*/	public void loadExcel(){
		int result =chooser.showOpenDialog(this);
		if(result==JFileChooser.APPROVE_OPTION){
			File file = chooser.getSelectedFile();
			FileInputStream fis=null;
			StringBuffer cols = new StringBuffer();
			StringBuffer data = new StringBuffer();
			
			try {
				fis =new FileInputStream(file);
				
				HSSFWorkbook book =null;
				book = new HSSFWorkbook(fis);
				
				
				HSSFSheet sheet = null;
				sheet = book.getSheet("��������");
				
				
				int total = sheet.getLastRowNum();
				DataFormatter df=new DataFormatter(); //�������� �ڷ����� ����ȭ�����ִ°�...(����ġ �ڷ���..)
				
				PreparedStatement pstmt=null;
				String sql="";
				
				/*---------------------------------------------
				 *  ù��° row�� �����Ͱ� �ƴ� �÷� �����̹Ƿ� 
				 *  ���������� �����Ͽ� insert into table(~~~~)�� ����
				 * --------------------------------------------*/
				
				System.out.println("�� ������ ù��° row��ȣ��? " + sheet.getFirstRowNum());
				HSSFRow firstRow = sheet.getRow(sheet.getFirstRowNum());
				//Row�� ������� �÷��� �м�����
				firstRow.getLastCellNum(); //������ �� �ѹ�
				cols.delete(0, cols.length());
				
				for(int i=0;i<firstRow.getLastCellNum();i++){
					HSSFCell cell = firstRow.getCell(i);
					if(i <firstRow.getLastCellNum()){
						cols.append(cell.getStringCellValue()+",");
						System.out.print(cell.getStringCellValue()+",");
					}else{
						
						cols.append(cell.getStringCellValue());
						System.out.print(cell.getStringCellValue());
					}
						
				}
				
				for(int i=1; i<=total;i++){
					HSSFRow row =sheet.getRow(i);
					int columnCount= row.getLastCellNum();
					
					
					for(int j=0;j<columnCount;j++){
						HSSFCell cell =row.getCell(j);
						//�ڷ����� ���ѵ��� �ʰ� ��� Stringó�� �� �� �ִ�.						
						if(j<columnCount-1){
							data.append(df.formatCellValue(cell)+",");
						}else{
							data.append(df.formatCellValue(cell));
						}
					}
					sql="insert into hospital("+cols.toString()+") values("+data.toString()+")";
					System.out.println("");
					System.out.println(sql);
					pstmt=con.prepareStatement(sql);
					int result2 = pstmt.executeUpdate(); //��������
					
					System.out.println(sql);
					value.removeAll(value);
					System.out.println("");
				}				
				JOptionPane.showMessageDialog(this, "�Է¿Ϸ�");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}

	//��� ���ڵ� ��������!!
	public void getList(){
		String sql="select * from hospital order by seq asc";
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		
		try {
			pstmt=con.prepareStatement(sql);
			rs= pstmt.executeQuery();
			//�÷��� ��������..
			ResultSetMetaData meta=  rs.getMetaData();
			int count = meta.getColumnCount();
			ColumnName = new Vector();
			for(int i=0;i<count;i++){
				ColumnName.add(meta.getColumnName(i+1));
			}
			
			
			list = new Vector<Vector>(); //2���� ����		
			
			while(rs.next()){ 
				Vector vec = new Vector(); //1���� ���� ...
				vec.add(rs.getString("seq"));
				vec.add(rs.getString("name"));
				vec.add(rs.getString("addr"));
				vec.add(rs.getString("regdate"));
				vec.add(rs.getString("status"));
				vec.add(rs.getString("dimension"));
				vec.add(rs.getString("type"));
				list.add(vec);
			}
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try {
				if(rs!=null){
					rs.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				if(pstmt!=null){
					pstmt.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	//������ ���ڵ����
	public void delete(){
		
		
	}
	
	
	public void actionPerformed(ActionEvent e) {
		Object obj = e.getSource();
		if(obj==bt_open){
			open();
		}else if(obj==bt_load){
			load();			
		}else if(obj==bt_excel){
			loadExcel();
		}else if(obj==bt_del){
			delete();
		}
		
	}
	
	
	public void init(){
		//DB ���ؼ� ���� ����
		con=manager.getConnection();
		
	}
	
	//���̺� ���� �����Ͱ��� ������ �߻��ϸ� �� ������...�����ϴ� ������..
	public void tableChanged(TableModelEvent e) {
		//�ĸ�, ������ ��ġ...
		//update�� ����...
		//update hospital set �÷��� = ��! where seq������...�����°͸�...�Ƶ����..
		
		//String sql="update hospital set ";
		System.out.println(e.getColumn()+"��Ʈ�ο�?"+e.getLastRow()); 
		
		System.out.println(ColumnName.elementAt(e.getColumn())); //������ Į������
		String set_val = (String)ColumnName.elementAt(e.getColumn());
		String seq_val = (String)ColumnName.elementAt(e.getLastRow());
		

		
		
		//System.out.println("ddd"+set_val2+"Ddd"+seq_val2);
		//System.out.println((list.get(set_val2).get(seq_val2)));
		
		
		//System.out.println(list.get(ColumnName.elementAt(e.getColumn())));
		


		String sql = "update hospital set "+set_val+" = "+set_val +" where "+seq_val+" = ";
		System.out.println(set_val+"�¹�"+seq_val+"seq");
		
		System.out.println((list.get(0)).get(3));
	}
	
	public static void main(String[] args) {
		new LoadMain();
	}
}
