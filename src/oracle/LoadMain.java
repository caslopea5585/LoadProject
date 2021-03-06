package oracle;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

public class LoadMain extends JFrame implements ActionListener,TableModelListener,Runnable{
	JPanel p_north;
	JTextField t_path;
	JButton bt_open, bt_load,bt_excel,bt_del;
	JTable table;
	JScrollPane scroll;
	JFileChooser chooser;
	FileReader reader= null;
	BufferedReader buffr = null;
	Connection con; //윈도우 창이 열리면 이미 접속을 확보해놓자
	DBManager manager=DBManager.getInstance();
	ArrayList<String> value= new ArrayList<String>();
	Vector<Vector> list;
	Vector ColumnName;
	MyModel myModel;
	Thread thread; //엑셀 등록시 사용될 쓰레드....
							//데이터량이 너무 많을 경우, 네트워크 상태가 좋지 않을경우 insert가 while문 속도를 못따라간다.
						//따라서 안정성을 위해 일부러 시간지연을 일으킨다...
	StringBuffer insertSql = new StringBuffer();//엑셀파일에 의해 생성된 쿼리문을 쓰레드가 사용할 수 있는 상태로 저장해놓자...
	String seq;
	
	public LoadMain() {
		p_north = new JPanel();
		t_path=new JTextField(25);
		bt_open=new JButton("CSV파일열기");
		bt_load=new JButton("로드하기");
		bt_excel= new JButton("엑셀로드");
		bt_del=new JButton("삭제하기");
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
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				JTable t = (JTable)e.getSource();
				
				int row = t.getSelectedRow();
				int col =0; //seq를 기준으로해서 삭제할꺼니깐... 무조건 0으로두자.
				
				seq = (String)t.getValueAt(row, 0);
					
			}
		});
		
		
		//윈도우와 리스너의 연결
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				manager.disConnect(con); 	//데이터베이스 자원 해제
				System.exit(0); 				//프로세스 종료
			
			}
		});
		
		setVisible(true);
		setSize(800, 600);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		init();
		
	}
	
	//파일 탐색기 띄우기
	public void open(){
		int result = chooser.showOpenDialog(this);
		//열기를 누르면 목적파일의 스트림을 생성하자.
		if(result==JFileChooser.APPROVE_OPTION){
			File file = chooser.getSelectedFile(); //유저가 선택한 파일
			String ext = FileUtil.getExt(file.getName());
			
			if(!ext.equals("csv")){
				JOptionPane.showMessageDialog(this, "CSV만 넣어주세요!");
				return;//진행막자
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
	
	//CSV -> Oracle데이터 이전(migration)하기
	public void load(){
		//버퍼 스트림을 이용하여 csv의 데이터를 한줄씩 읽어들여 insert시키자.
		//레코드가 없을때까지...while문으로 돌리면 너무 빠르므로...네트웍이 감당 불가
		//일부러...지연시키면서 보내자 (속도싱크가 맞지 않으면 데이터가 누락 될 수 있음)
		String data;
		StringBuffer sb = new StringBuffer();
		PreparedStatement pstmt=null;
		
		try {
			
			while(true){
				data = buffr.readLine();	
				if(data==null)break;
				String[] value = data.split(",");
				if(!value[0].equals("seq")){ //발견되지 않을경우에만 insert할꺼야~
					sb.append("insert into hospital(seq,name,addr,regdate,status,dimension,type)");
					sb.append(" values("+value[0]+",'"+value[1]+"','"+value[2]+"','"+value[3]+"','"+value[4]+"',"+value[5]+",'"+value[6]+"')");
					
					pstmt=con.prepareStatement(sb.toString());
					int result = pstmt.executeUpdate(); //쿼리수행
					System.out.println(sb.toString());
					//기존에 누적된 StringBuffer의 데이터를 모두지우기
					sb.delete(0, sb.length());
					
				}else{
					System.out.println("난제외");
				}
			}
			JOptionPane.showMessageDialog(this, "마이그레이션 완료!!");
			
			//JTable나오게 처리!!
			getList();
			table.setModel(myModel = new MyModel(list, ColumnName));
			table.getModel().addTableModelListener(this);//테이블 모델과 리스너와의 연결(테이블 모델과 리스너 연결이므로..테이블에서 쓰고 있는 모델을 받아와서 거기에 리스너를 붙여야함)
			//물론 Mymodel을 전역으로 빼서 해도되긴함...단 이런경우 시점상의 차이가 발생할 수 도 있음.
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
	
/*	//엑셀파일 읽어서 db에 마이그레이션 하기...
	//javaSE 엑셀제어 라이브러리 있다??? X
	//openSource  공개소프트웨어
	//copyright(소프트의무료화) <----> copyleft(소프트화무료화 / 아파치단체)
	//POI 라이브러리!! http://apache.org
	 * (엑셀파일 제어하기 위해 들어가는 순서)
	 * HSSFWorkBook : 엑셀파일
	 * HSSFSheet : 시트(sheet)
	 * HSSFRow : 한 줄...row
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
				sheet = book.getSheet("동물병원");
				
				
				int total = sheet.getLastRowNum();
				DataFormatter df=new DataFormatter(); //데이터의 자료형을 포맷화시켜주는거...(아파치 자료형..)
				
				PreparedStatement pstmt=null;
				String sql="";
				
				/*---------------------------------------------
				 *  첫번째 row는 데이터가 아닌 컬럼 정보이므로 
				 *  이정보들을 추출하여 insert into table(~~~~)에 넣자
				 * --------------------------------------------*/
				
				System.out.println("이 파일의 첫번째 row번호는? " + sheet.getFirstRowNum());
				HSSFRow firstRow = sheet.getRow(sheet.getFirstRowNum());
				//Row을 얻었으니 컬럼을 분석하자
				firstRow.getLastCellNum(); //마지막 셀 넘버
				cols.delete(0, cols.length());
				
				for(int i=0;i<firstRow.getLastCellNum();i++){
					HSSFCell cell = firstRow.getCell(i);
					if(i <firstRow.getLastCellNum()-1){
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
					
					data.delete(0, data.length());
					for(int j=0;j<columnCount;j++){
						HSSFCell cell =row.getCell(j);
						String value2 = df.formatCellValue(cell);
						//자료형에 국한되지 않고 모두 String처리 할 수 있다.
						if(cell.getCellType()==HSSFCell.CELL_TYPE_STRING){
							value2="'"+value2+"'";
						}
						
						if(j<columnCount-1){
							data.append(value2+",");
						}else{
							data.append(value2);
						}
					}
					sql="insert into hospital("+cols.toString()+") values("+data.toString()+");";
					insertSql.append(sql);

				}				
				//모든것이 끝낫으니 편안하게 쓰레드에게 일시키자!! 타이밍 문제 해결
				thread = new Thread(this); //쓰레드 자체의 run을 수행하는 것이 아니라.. 내 클래스의 run을 수행하게 된다!this을 넣는 이유(타켓!)
				thread.start();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
	}

	//모든 레코드 가져오기!!
	public void getList(){
		String sql="select * from hospital order by seq asc";
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		
		try {
			pstmt=con.prepareStatement(sql);
			rs= pstmt.executeQuery();
			//컬럼명도 추출하자..
			ResultSetMetaData meta=  rs.getMetaData();
			int count = meta.getColumnCount();
			ColumnName = new Vector();
			for(int i=0;i<count;i++){
				ColumnName.add(meta.getColumnName(i+1));
			}
			
			
			list = new Vector<Vector>(); //2차원 벡터		
			
			while(rs.next()){ 
				Vector vec = new Vector(); //1차원 벡터 ...
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
	
	//선택한 레코드삭제
	public void delete(){
		PreparedStatement pstmt=null;
		
		int ans = JOptionPane.showConfirmDialog(LoadMain.this, seq+"선택한 레코드 삭제할래요?");
		if(ans==JOptionPane.OK_OPTION){
			String sql ="delete from hospital where seq = "+seq;
			System.out.println(sql);
			try {
				pstmt=con.prepareStatement(sql);
				int result = pstmt.executeUpdate();
				if(result!=0){
					JOptionPane.showMessageDialog(this, "삭제완료");
					getList();
					//방금완성된 list를 다시 Mymodel클래스에 대입
					myModel.list=list;
					table.updateUI();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}finally{
				if(pstmt!=null){
					try {
						pstmt.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
			
		}
		
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
		//DB 컨넥션 얻어다 놓기
		con=manager.getConnection();
		
	}
	
	//테이블 모델의 데이터값에 변경이 발생하면 그 찰나를...감지하는 리스너..
	public void tableChanged(TableModelEvent e) {
		//컴마, 수정한 위치...
		//update문 수정...
		//update hospital set 컬럼명 = 값! where seq값으로...찍히는것만...아디오스..
		int row =table.getSelectedRow();
		int col = table.getSelectedColumn();
		PreparedStatement pstmt=null;		
		
		String column = (String)ColumnName.elementAt(col);		
		String value = (String)table.getValueAt(row, col);//지정한 좌표의 값 반환
		String seq = (String)table.getValueAt(row, 0);
		String sql ="update hospital set "+column+ "='"+value+"'";
		sql+=" where seq="+seq;
		
		System.out.println(sql);
		try {
			pstmt= con.prepareStatement(sql);
			int result = pstmt.executeUpdate();
			if(result!=0){
				JOptionPane.showMessageDialog(this, "수정완료");
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		} finally{
			if(pstmt!=null){
				try {
					pstmt.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		
		}
		
		
	}
	
	public void run() {
		PreparedStatement pstmt=null;
		
		
		//insertSql에 insert문이 몇개인지 알아보자
		String[] str = insertSql.toString().split(";");
		System.out.println("인서트 문의 숫자는 = "+ str.length);
		for(int i=0 ; i<str.length;i++){
			
			try {
				thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				pstmt = con.prepareStatement(str[i]);
				int result = pstmt.executeUpdate();
				System.out.println("찍는중" + i+"번째");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		//기존에 사용했던 StringBuffer 비우기
		insertSql.delete(0, insertSql.length());
		if(pstmt!=null){
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	public static void main(String[] args) {
		new LoadMain();
	}
}
