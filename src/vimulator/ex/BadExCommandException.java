package vimulator.ex;

import org.gjt.sp.jedit.View;

public class BadExCommandException extends Exception
{
	public BadExCommandException(View view, String command)
	{
		this.view = view;
		this.command = command;
	}

	public View getView()
	{
		return view;
	}

	public String getCommand()
	{
		return command;
	}

	private View view;
	private String command;
}
