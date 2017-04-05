//JTable�� ���÷� ������ ���� ��Ʈ�ѷ�
package oracle;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class MyModel extends AbstractTableModel {
	Vector ColumnName; //�÷��� ������ ���� ����
	Vector<Vector> list; //���ڵ带 ���� 2���� ����
	
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

	//���̺��� ���̺�𵨿� �������ִ�..���� ���̺��� �����ϰ��Ϸ��� �������̵� �޼��带 ȣ���ؼ� true�� �����ϰ��ؾ��Ѵ�.
	//(row,col)�� ��ġ�� ���� ���������ϰ� �Ѵ�.
	public boolean isCellEditable(int row, int col) {
		 
		return true;
	}
	
	//������ ���� ����� �� �ֵ���...�������̵� �޼���
	public void setValueAt(Object value, int row, int col) {
		//�� ȣ���� �����Ѵ�.
		Vector vec = list.get(row); //1���� ���� ��ȯ... �� ���ڵ带 �ǹ�
		vec.set(col, value);
		this.fireTableCellUpdated(row, col);
	}
	
}

