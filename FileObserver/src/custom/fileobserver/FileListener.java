package custom.fileobserver;

public interface FileListener {
	public void fileCreated(String name);
	public void fileDeleted(String name);
	public void fileModified(String name);
	public void fileRenamed(String oldName, String newName);
}
