package eclipsematrix.jobs;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.PlatformUI;

import eclipsematrix.entities.ConfigFileRecord;
import eclipsematrix.entities.ConfigFileRecord.ImportState;
import eclipsematrix.notifier.Notifier;
import eclipsematrix.utils.MQLUtil;

/**
 * 
 * @author Hannes Lenke hannes@lenke.at
 *
 */
public class MQLInstallJob extends Job {

	/**
	 * holds the FileList.
	 */
	private List<ConfigFileRecord> mxList ;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            Name of the Task.
	 * @param mxList
	 *            get The List.
	 */
	public MQLInstallJob(final String name,
			final List<ConfigFileRecord> mxList) {
		super(name);
		this.mxList = new LinkedList<ConfigFileRecord>(mxList);
	}

	@Override
	protected final IStatus run(final IProgressMonitor monitor) {
		monitor.beginTask("Install file(s)", 100);
		monitor.subTask("get context");
		MQLUtil mql = new MQLUtil();
		monitor.worked(20);
		// count files
		int y = 0;
		for (int i = 0; i < mxList.size(); i++) {
			ConfigFileRecord mxFile = mxList.get(i);
			if (mxFile.isChanged()) {
				y++;
			}
		}
		monitor.worked(10);
		if (y > 0) {
			// calculate subTaskAmount based on file count
			int subTaskAmount = 70 / y;

			for (int i = 0; i < mxList.size(); i++) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}

				final ConfigFileRecord mxFile = mxList.get(i);
				if (mxFile.isChanged()) {
					try {
						System.out.println("Import " + mxFile.getPath());
						mql.genericImport(mxFile.getPath());
						PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
							public void run() {
								mxFile.setChanged(Boolean.FALSE);
								mxFile.setState(ImportState.NORMAL);
							}
						});
						monitor.worked(subTaskAmount);
					} catch (final Exception e) {
						e.printStackTrace();
						PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
							public void run() {
								mxFile.setState(ImportState.ERROR);
								Notifier.logError("Error during Import ", e.getMessage());
							}
						});
					}
				}
			}
		}
		monitor.done();
		return Status.OK_STATUS;
	}
}
