package main.java.aftermath.engine;

import java.util.ArrayList;
import java.util.List;

import main.java.aftermath.controllers.AftermathController;
import main.java.aftermath.vehicles.Transport;
import main.java.encephalon.dto.MapVertex;
import main.java.encephalon.dto.MapEdge.RoadTypes;
import main.java.encephalon.profiler.Task;

public class AftermathEngine implements Runnable {
	private static final int NUMBER_OF_VEHICLES = Integer.valueOf(System.getProperty("aftermath.map.vehicles"));
	private AftermathController controller;
	private List<Transport> transporters;

	public AftermathEngine(AftermathController aftermathController)
	{
		this.controller = aftermathController;
		this.transporters = new ArrayList<Transport>();
	}

	@Override
	public void run()
	{
		try {
			int tally = 0;
			long l = 0L;

			while(tally < NUMBER_OF_VEHICLES)
			{
				RoadTypes roadType = controller.getEdgeData().get(l).getMode();
				switch(String.valueOf(roadType))
				{
				case "secondary":
				case "primary":
				case "tertiary":
				case "residential":
				case "service":
				case "motorway":
					transporters.add(new Transport(controller, l));
					tally++;
					break;
				default:
					break;
				}
				l++;
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			System.out.println("Null Edge!  Did OSMReader not finish loading and the engine tried to run?");
			e1.printStackTrace();
		}

		while(true)
		{
			MapVertex mvv = this.controller.getMapData().get(630971621L);
			
			try {
				Thread.sleep(250);
				traverse();
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
				new Task(controller.getProfiler(), null, "Aftermath Engine Exception on traverse()", null).end();
			}
		}
	}

	private void traverse() throws Exception
	{
		Task task = new Task(controller.getProfiler(), null, "Aftermath Engine Tick", null);
		for(Transport t : transporters)
		{
			try
			{
				t.traverse();
			}
			catch(Throwable th)
			{
				th.printStackTrace();
			}
			finally
			{
				task.end();
			}
		}
	}

	public List<Transport> getTransporters()
	{
		return transporters;
	}
}
