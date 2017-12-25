package main.java.aftermath.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import main.java.aftermath.controllers.AftermathController;
import main.java.aftermath.vehicles.Transport;
import main.java.encephalon.dto.Coordinates;
import main.java.encephalon.dto.MapEdge.RoadTypes;
import main.java.encephalon.profiler.Task;

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
