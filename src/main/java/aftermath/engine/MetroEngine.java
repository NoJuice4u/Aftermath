package main.java.aftermath.engine;

import main.java.aftermath.controllers.AftermathController;

public class MetroEngine implements Runnable {

	public MetroEngine(AftermathController aftermathController)
	{

	}

	@Override
	public void run()
	{
		try {
		} catch (Exception e1) {
		}

		while(true)
		{
			try {
				Thread.sleep(60000);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
