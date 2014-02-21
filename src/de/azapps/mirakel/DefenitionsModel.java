package de.azapps.mirakel;

import de.azapps.mirakel.model.task.Task;


public class DefenitionsModel {
	public interface ExecInterfaceWithTask {
		public void exec(Task task);
	}
}
