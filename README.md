#d4move#

Visualizing Orange D4D Call Data Record Data using WebGL/Threejs

(c) Clas Rydergren, Linköping University, Sweden, clas.rydergren@liu.se


The tool consists of two parts, one Java script for parsing the data into time sliced segments, and one HTML/Javascript page for the visualization.
The parsing script takes input data from a PostgreSQL database, with tables similar to the Orange D4D data (see below). The output is a JSON structure. 
The HTML/Javascript page makes use of Threejs as a layer on top of a Google Map for the visualization. The page takes the JSON as input.

See http://www.d4d.orange.com/home for information about the data.

Email me on clas.rydergren@liu.se for screenshots.


##Parsing##

The Java script for generating a JSON structure suitable for visualizing of D4D-movement data.

The script outputs a JSON structure with the data:

* latlon - a list of coordinates
* segments - pointer to the start of the coordinate list
* segmentslength - cumulative length of coordinates for each segment

The output is written to a file moveLatLon.js.

##Visualization##

The JSON structure is utilized in the WebGL-visualization/HTML. The complete JSON file is loaded at page load.
The visualization cycles through each time slice, plotting the movement form antenna to antenna.
One line represents the start of a trip, the actual travel time is not considered and the line
is draw in the start time slice, from the last known position, with intermediate point if avaiable during the given time slice (hour), 
to the next known antenna position.

The visualization draws thick lines (two WebGL triangles) with alpha blending, resulting in brighter lines representing a larger number of movementens.

The visualization can hande up to a few thousand lines with good framerate, after that the animation is somewhat laggy.

##Set up##

###Database configuration###

The database name, user and credentials are located in the db.properties file.


###Configuration and Dependencies###

The script connects to a PostreSQL database with a senegal_set2_small table and a sengal_ant_table.
The script uses the postgresql-9.3-1102.jdbc41.jar for the connection to the database.

The senegal_set2_small table is defined as:

create table senegal_set2_small (
id serial primary key,
user_id int not null,
"time" timestamp not null,
antenna_id int
);

This table can be populated with the following COPY command

copy senegal_set2(user_id,time,antenna_id) from 'SET2_P01.CSV' delimiter as ',';

The senegal_ant_pos table is defined as:

create table senegal_ant_pos (
id serial primary key,
site_id int not null,
arr_id int not null,
lon real,
lat real
);

This table can be populated with the following COPY command

copy senegal_ant_pos(site_id,arr_id,lon,lat) from 'SITE_ARR_LONLAT.CSV' with csv header delimiter as ',';

In order to have the script to run resonably fast, make sure to have index on columns like:

CREATE INDEX user_idx ON senegal_set2_small USING btree ("user_id");
CREATE INDEX user_idx ON senegal_set2_small USING btree ("antenna_id");
CREATE INDEX user_idx ON senegal_ant_pos USING btree ("site_id");


###Serving the web page###

Point the web server (or the browser directly, since no server side scripting is made) to the index.html in the web directory.