/*
    TimisoaraPublicTransport - display public transport information on your device
    Copyright (C) 2011  Mihai Balint

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/
package ro.mihai.tpt;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ro.mihai.tpt.R;
import ro.mihai.tpt.conf.PathStationsSelection;
import ro.mihai.tpt.conf.StationPathsSelection;
import ro.mihai.tpt.conf.StationPathsSelection.Node;
import ro.mihai.tpt.model.*;
import ro.mihai.tpt.utils.AndroidSharedObjects;
import ro.mihai.tpt.utils.CityActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;

public class ViewTimes extends CityActivity {
	private TableLayout timesTable;
	private LayoutInflater inflater;
	private City city;
	private PathStationsSelection path;
	private UpdateTimes updater;
	private UpdateQueue queue;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
    	setContentView(R.layout.list_times);
    	city = getCity();
    	city.getClass();
    	
    	path = new PathStationsSelection(AndroidSharedObjects.instance().getLinePath());
    	path.selectAllStations();
		queue = new UpdateQueue();
    	Button update = (Button)findViewById(R.id.UpdateButton);
    	update.setOnClickListener(updater = new UpdateTimes());
    	update.setText(path.getLabel());
    	
    	timesTable = (TableLayout)findViewById(R.id.StationTimesTable);
    	inflater = this.getLayoutInflater();
    	inflateTable();
    }
    
    public void queueUIUpdate(Runnable r) {
    	queue.add(r);
    	runOnUiThread(queue);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	updater.killUpdate();
    }

	private void inflateTable() {
		timesTable.removeAllViews();
		List<StationPathsSelection> stations = path.getStations();

		for(StationPathsSelection sel: stations) {
			Station s = sel.getStation();
			Estimate est = path.getEstimate(s);
			
	    	//if (est.isVehicleHere()) {
	    	//	timesTable.addView(inflater.inflate(R.layout.times_station_vehicle, timesTable, false));
	    	//}
	    	timesTable.addView(newStationEstimateView(est));
	    	
	    	for(Node connection : sel.getConnections()) {
	    		Path connectingPath = connection.path;
	    		est = connectingPath.getEstimate(connection.station);

	    		timesTable.addView(newConnectionEstimateView(est));
	    	}
		}
	}

	private View newStationEstimateView(Estimate est) {
		int rowLayout;
		if (est.isUpdating())
			rowLayout = R.layout.times_station_updating;
		else if (est.hasErrors()) {
			rowLayout = R.layout.times_station_err;
			updater.setHasErrors();
		} else
			rowLayout = R.layout.times_station;

		View timesRow = inflater.inflate(rowLayout, timesTable, false);
		String label = est.getStation().getNicestNamePossible();
		TextView stationLabel = (TextView)timesRow.findViewById(R.id.StationLabel);
		stationLabel.setText("|"+label);
		
		TextView stationTime = (TextView)timesRow.findViewById(R.id.StationTime);
		stationTime.setText(est.estimateTimeString());

		return timesRow;
	}

	private View newConnectionEstimateView(Estimate est) {
		int rowLayout;
		if (est.isUpdating())
			rowLayout = R.layout.times_connection_updating;
		else if (est.hasErrors()) {
			rowLayout = R.layout.times_connection_err;
			updater.setHasErrors();
		} else
			rowLayout = R.layout.times_connection;

		View timesRow = inflater.inflate(rowLayout, timesTable, false);
		
		Path connectingPath = est.getPath();
		
		TextView lineNameLabel = (TextView)timesRow.findViewById(R.id.LineName);
		lineNameLabel.setText(connectingPath.getLine().getName());

		TextView lineDirectionLabel = (TextView)timesRow.findViewById(R.id.LineDirection);
		lineDirectionLabel.setText(connectingPath.getNiceName());
		
		TextView stationTime = (TextView)timesRow.findViewById(R.id.StationTime);
		stationTime.setText(est.estimateTimeString());

		return timesRow;
	}
	
	private class UpdateTimes implements Runnable, OnClickListener {
		private AtomicBoolean running = new AtomicBoolean(false);
		private AtomicBoolean hasErrors = new AtomicBoolean(false);
		
		public void run() {
			// XXX
			int ec = 0, index = 0;
			for(StationPathsSelection sel: path.getStations()) {
				if(!running.get()) return;
				Station s = sel.getStation();
				ec = updateStationRowView(ec, index, path.getPath(), s);
				index++;
				for(Node connection : sel.getConnections()) {
					if(!running.get()) return;
					ec = updateConnectionRowView(ec, index, connection.path, connection.station);
					index++;
				}
			}
			killUpdate();
			runOnUiThread(new UpdateView());
			if (hasErrors.compareAndSet(true, false))
				runOnUiThread(new ReportError());
		}

		private int updateConnectionRowView(int ec, final int rowIndex, Path path, Station s) {
			final Estimate est = path.getEstimate(s);
			Runnable upd = new Runnable() {
				public void run() {
					timesTable.removeViewAt(rowIndex);
		    		timesTable.addView(newConnectionEstimateView(est), rowIndex);
				}
			};
			est.startUpdate();
			queueUIUpdate(upd);
			ec = path.updateStation(ec, s);
			queueUIUpdate(upd);
			return ec;
		}

		private int updateStationRowView(int ec, final int rowIndex, Path path, Station s) {
			final Estimate est = path.getEstimate(s);
			Runnable upd = new Runnable() {
				public void run() {
					timesTable.removeViewAt(rowIndex);
		    		timesTable.addView(newStationEstimateView(est), rowIndex);
				}
			};
			est.startUpdate();
			queueUIUpdate(upd);
			ec = path.updateStation(ec, s);
			queueUIUpdate(upd);
			return ec;
		}
		
		@Deprecated
		private int update(int ec, UpdateView viewUpdater, Path path, Station s) {
			path.getEstimate(s).startUpdate();
			runOnUiThread(viewUpdater);
			ec = path.updateStation(ec, s);
			return ec;
		}
		
		public void onClick(View v) {
			if (running.compareAndSet(false, true)) 
				new Thread(this).start();
		}
		
		public void killUpdate() {
			running.set(false);
			path.clearAllUpdates();
		}
		
		public void setHasErrors() {
			hasErrors.set(true);
		}
	}
	
    private class UpdateView implements Runnable {
    	private long last=0;
    	
		public void run() {
			long crt = System.currentTimeMillis(); 
			if(crt>last && (crt-last)<500) return;
			last = crt;
			inflateTable();
		}
	}
    
    private class ReportError implements Runnable, DialogInterface.OnClickListener {
    	public void run() {
			new AlertDialog.Builder(ViewTimes.this)
				.setMessage(R.string.upd_error)
				.setPositiveButton("Ok", this)
				.create()
				.show();
		}
    	
		public void onClick(DialogInterface dialog, int which) {
			// NOP
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.times_menu, menu);
        return true;
    }    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.select_connections:
            showSelectConnections();
            return true;
        case R.id.view_map:
            //showHelp();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    public void showSelectConnections() {
    	path.addConnections(city.getLine("33").getFirstPath());
    	runOnUiThread(new UpdateView());
    }
}