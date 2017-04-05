//JTable이 수시로 정보를 얻어가는 컨트롤러
package oracle;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class MyModel extends AbstractTableModel {
	Vector ColumnName; //컬럼의 제목을 담을 백터
	Vector<Vector> list; //레코드를 담을 2차원 벡터
	
	public MyModel(Vector list, Vector ColumnName) {
		this.list = list;
		this.ColumnName=ColumnName;
		
	}
	
	public int getRowCount() {
	
		return list.size();
	}

	public int getColumnCount() {

		return ColumnName.size();
	}
	
	
	public String getColumnName(int col) {
		return (String)ColumnName.elementAt(col);
	
	}

	public Object getValueAt(int row, int col) {
		Vector vec = list.get(row);
		
		return vec.elementAt(col);
	}

	//테이블은 테이블모델에 의존해있다..따라서 테이블을 수정하게하려면 오버라이드 메서드를 호출해서 true를 가능하게해야한다.
	//(row,col)에 위치한 셀을 편집가능하게 한다.
	public boolean isCellEditable(int row, int col) {
		 
		return true;
	}
	
	//수정한 값이 적용될 수 있도록...오버라이드 메서드
	public void setValueAt(Object value, int row, int col) {
		//층 호수를 변경한다.
		Vector vec = list.get(row); //1차원 벡터 반환... 한 레코드를 의미
		vec.set(col, value);
		this.fireTableCellUpdated(row, col);
	}
	
}

