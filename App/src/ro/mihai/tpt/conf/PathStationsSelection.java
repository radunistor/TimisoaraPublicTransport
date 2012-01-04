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
package ro.mihai.tpt.conf;

import java.util.ArrayList;
import java.util.List;

import ro.mihai.tpt.model.Estimate;
import ro.mihai.tpt.model.Path;
import ro.mihai.tpt.model.Station;

public class PathStationsSelection {
	private Path path;
	private List<StationPathsSelection> stations;
	
	//  hh:mm MainStation1 [hide/show]
	//			hh:mm Line1 (dir) [hide/show]
	//			hh:mm Line2 (dir) [hide/show]
	//			hh:mm Line3 (dir) [hide/show]
	//  hh:mm MainStation2 [hide/show]
	//			hh:mm Line2 (dir) [hide/show]
	//			hh:mm Line4 (dir) [hide/show]
	//  hh:mm MainStation3 [hide/show]
	//  hh:mm MainStation4 [hide/show]
	//			hh:mm Line1 (dir) [hide/show]
	//  hh:mm MainStation5 [hide/show]
	//  hh:mm MainStation6 [hide/show]

	public PathStationsSelection(Path path) {
		this.path = path;
		this.stations = new ArrayList<StationPathsSelection>();
	}
	
	public List<StationPathsSelection> getStations() {
		return stations;
	}
	public Path getPath() {
		return path;
	}
	public Estimate getEstimate(Station s) {
		return path.getEstimate(s);
	}
	public void clearAllUpdates() {
		path.clearAllUpdates();
		for(StationPathsSelection sel : stations) {
			Station s = sel.getStation();
			for(Path connection : sel.getConnections())
				connection.getEstimate(s).clearUpdate();
		}
	}
	public String getLabel() {
		return path.getLine().getName()+" ("+path.getNiceName()+")";
	}

	public void clearSelection() {
		this.stations.clear();
	}
	
	public void selectAllStations() {
		List<Station> stations = path.getStationsByPath();
		for(Station s : stations)
			this.stations.add(new StationPathsSelection(s));
	}
	
}
