/*���ϰ� ���õ� �۾��� �����ִ� ���뼺�ִ� Ŭ���� �����Ѵ�*/
package util.file;
public class FileUtil {
	
	public static String getExt(String path){/*�Ѱܹ޴� ��ο��� Ȯ���� ���ϱ�*/
		int last =path.lastIndexOf(".");
		return path.substring(last+1,path.length());
	}
}
