<!DOCTYPE html>
<!--
//======================================================================================== 
//    HTML5 Webpage for the Capri 25 Offline Race Team
//	
//    charts.php - wind data charts using Google Chart API
//
//	  losely build upon the free, responsive Corlate template
//	
//    (c) KaiserWare, Volker Petersen, January 2016
//
//======================================================================================== 
-->
<?php 
global $title;
$race_id;
$title = 'Offline - Wind Charts';
require "offline_header.php";
require "bin/db_connect.php";

$race_js = fetch_races();
// get the wind data tables from the MYSQL database
if (isset($_GET["race"])) {
	$GLOBALS['$race_id'] = $_GET["race"];
}

?>
<script type="text/javascript" src="https://www.google.com/jsapi"></script>

<section id="feature" class="body-page-start">
	<div class="container">
		<div class="row">
		    <div class="col-xs-12 col-sm-9 text-center">
				<h1>Race Course Wind Data</h1>
				<h4 id="chart_title"></h4>
			</div>
			<div class="col-xs-12 col-sm-3 text-right" id="raceSelectionDropdown"></div>
			<div class="col-xs-12 col-sm-3 text-right" id="chartSelectionDropdown">
				<select id='charttype_selection'>
					<option value="allQuadrants">All Quadrants combined</option>
					<option value="perQuadrant">One Line per Quadrant</option>
					<option value="windSpeed">AWS & TWS</option>
					<option value="windDirection">AWD & TWD</option>
					<option value="trueDirectionSpeed" selected>TWD & TWS</option>
					<option value="appDirectionSpeed">AWD & AWS</option>
				</select>
			</div>
		</div>
		<div class="row">
				<!-- Google Chart goes in div with id=char_div -->
			<div class="col-xs-12">
				<div id="chart_div1" style="width: 100%; height: 550px;"></div>
			</div>
			<div class="col-xs-12">
				<div id="chart_div2" style="width: 100%; height: 0px;"></div>
			</div>
		</div>
		<div class="row">
			<div class="col-xs-12"><p>&nbsp;</p>
		</div>
	</div>
</section>


<script type='text/javascript'>
	// the php script db_connect.php generates a javascript array js_wind[][] which conatins for each 
	// db wind entry js_wind[i][Date, a, b, c, d, TWD, TWS, AWD, AWS]
	//                         [ 0    1  2  3  4   5    6    8    8 ] 
	//
	var alpha = 0.3;
	var RANGE = 900;  // 900 * 4 = 3600 / 60 = 60 min
	var SHORT = 75;  //   75 * 4 =  600 / 60 =  5 min

	function plot_chart(chart_data, options, title, yAxis, legend, twoCharts) {
		if (typeof twoCharts == 'undefined')
			twoCharts = false;

		options.hAxis = {
			format: 'yyyy-M-dd HH:mm',
			title: 'Race Date / Time (UTC)'
		};
		if (yAxis != "") {
			options.vAxis = {
				format: '0',
				title: yAxis
			};
	        options.chartArea = {left: 70, right: 20, top: 70, width: '100%'};
		} else {
	        options.chartArea = {left: 70, right: 70, top: 70, width: '100%'};
		}
		if (legend) {
			options.legend = {
				position: 'top', 
				alignment: 'center'
			};
		}
		options.smoothLine = false;
		options.explorer = {};
		
		document.getElementById("chart_title").innerHTML = title;
		if (twoCharts) {
			jQuery('#chart_div2').css('height', '550px');
			var chart2 = new google.visualization.LineChart(document.getElementById('chart_div2'));			
			chart2.draw(chart_data, options);

		} else {
			jQuery('#chart_div2').empty();
			jQuery('#chart_div2').css('height', '0px');
			var chart1 = new google.visualization.LineChart(document.getElementById('chart_div1'));			
			chart1.draw(chart_data, options);
		}
	}

	function all_lines(legend) {
		var chart_data = new google.visualization.DataTable();
		chart_data.addColumn('datetime', 'Time of Day');
		chart_data.addColumn('number', 'Q1');
		chart_data.addColumn('number', 'Q2');
		chart_data.addColumn('number', 'Q3');
		chart_data.addColumn('number', 'Q4');
		chart_data.addColumn('number', 'All');
		for (var i=0; i < js_wind.length; i++){
			var row = [];
			for (var j = 0; j < 6; j++) {
				if (j == 0) {
					row.push(new Date(Date.parse(js_wind[i][j])));
					//console.log("all_lines: "+js_wind[i][j]+" | "+Date.parse(js_wind[i][j]));
				} else {
					row.push(js_wind[i][j]);
				}
			}
			chart_data.addRow(row);
		}
        var options = {
          series: {
            0: { color: 'FF0000' },
            1: { color: '00FF00' },
            2: { color: '0000FF' },
            3: { color: 'FFAA33' },
            4: { color: '000000', lineDashStyle: [10, 5] }
          }
        };
        var title = 'Quadrants 1, 2, 3, & 4 plus entire Race course wind data';
        var yAxis = 'True Wind Direction (True North)';
		plot_chart(chart_data, options, title, yAxis, legend);
	}

	function all_quadrants(legend) {
		var chart_data = new google.visualization.DataTable();
		chart_data.addColumn('datetime', 'Time of Day');
		chart_data.addColumn('number', 'All Quadrants');
		for (var i=0; i < js_wind.length; i++){
			chart_data.addRow([new Date(Date.parse(js_wind[i][0])), js_wind[i][5]]);
			//console.log("all Q: "+js_wind[i][0]+" | "+new Date(Date.parse(js_wind[i][0])));
		}
        var options = {
          series: {
            0: { color: '000000' }
           }
        };
        var title = 'Wind data from all Quadrants of the Race course';
        var yAxis = 'True Wind Direction (True North)';
		plot_chart(chart_data, options, title, yAxis, legend);
	}

	function aws_tws(legend) {
		var chart_data = new google.visualization.DataTable();
		chart_data.addColumn('datetime', 'Time of Day');
		chart_data.addColumn('number', 'AWS');
		chart_data.addColumn('number', 'TWS');
		for (var i=0; i < js_wind.length; i++){
			chart_data.addRow([new Date(Date.parse(js_wind[i][0])), js_wind[i][8], js_wind[i][6]]);
			//console.log("all Q: "+js_wind[i][0]+" | "+new Date(Date.parse(js_wind[i][0])));
		}
        var options = {
          series: {
            0: { color: '000000' },
            1: { color: '00FF00' },
           }
        };
        var title = 'True and Apparent Wind Speed';
        var yAxis = 'Wind Speed (kts)';
		plot_chart(chart_data, options, title, yAxis, legend);
	}

	function awd_twd(legend) {
		var chart_data = new google.visualization.DataTable();
		chart_data.addColumn('datetime', 'Time of Day');
		chart_data.addColumn('number', 'AWD');
		chart_data.addColumn('number', 'TWD');
		for (var i=0; i < js_wind.length; i++){
			chart_data.addRow([new Date(Date.parse(js_wind[i][0])), js_wind[i][7], js_wind[i][5]]);
			//console.log("all Q: "+js_wind[i][0]+" | "+new Date(Date.parse(js_wind[i][0])));
		}
        var options = {
          series: {
            0: { color: '000000' },
            1: { color: '00FF00' },
           }
        };
        var title = 'True and Apparent Wind Direction (TWD avg = '+avgTWDall+')';
        var yAxis = 'Wind Direction Difference from Average';
		plot_chart(chart_data, options, title, yAxis, legend);
	}

	function twd_tws(legend) {
		var avgTWD, avgShort, StdDev, upper, lower, start, start2, end;
		var chart_data1 = new google.visualization.DataTable();
		chart_data1.addColumn('datetime', 'Time of Day');
		chart_data1.addColumn('number', 'TWD');
		chart_data1.addColumn('number', 'smoothed');
		chart_data1.addColumn('number', 'avg TWD');
		chart_data1.addColumn('number', '+1 StdDev');
		chart_data1.addColumn('number', '-1 StdDev');

		var chart_data2 = new google.visualization.DataTable();
		chart_data2.addColumn('datetime', 'Time of Day');
		chart_data2.addColumn('number', 'TWS');

		var smoothed = 0.0;

		for (var i=0; i < js_wind.length; i++){
			end = i;
			start = i-RANGE;
			startShort = i-SHORT;
			if (start < 0) {
				start = 0;
			}
			if (startShort < 0) {
				startShort = 0;
			}
			if (i==0) {
				smoothed = js_wind[i][5];
				avgTWD = js_wind[i][5];
				avgShort = js_wind[i][5];
				StdDev = 0.0;
			} else {
				//smoothed = alpha*js_wind[i][5]+(1-alpha)*smoothed;
				smoothed = smoothWindDirection(smoothed, js_wind[i][5], alpha);
				avgTWD = avgWindDirection(start, end, 5);
				avgShort = avgWindDirection(startShort, end, 5);
				StdDev = StdDevWind(start, end, avgTWD, 5);
			}
			upper = avgTWD+StdDev;
			lower = avgTWD-StdDev;
			chart_data1.addRow([new Date(Date.parse(js_wind[i][0])), js_wind[i][5], avgShort, avgTWD, upper, lower ]);
			chart_data2.addRow([new Date(Date.parse(js_wind[i][0])), js_wind[i][6] ]);
		}
        var options = {
          series: {
            0: { color: '0000FF', lineWidth: 1, lineDashStyle: [2, 1] },
            1: { color: 'FF0000' },
            3: { color: '202020', lineWidth: 2, lineDashStyle: [4, 2] },
            4: { color: '202020', lineWidth: 2, lineDashStyle: [4, 2] },
            5: { color: '202020', lineWidth: 2, lineDashStyle: [4, 2] }
          }
        };
        var title = 'True Wind Direction and Speed (TWD avg = '+avgTWDall+')';
        var yAxis = 'True Wind Direction difference from Average TWD';
		plot_chart(chart_data1, options, title, yAxis, true);
		yAxis = 'True Wind Speed (kts)';
		plot_chart(chart_data2, options, title, yAxis, legend, true);
	}

	function awd_aws(legend) {
		var avgAWD, start, end;
		var chart_data1 = new google.visualization.DataTable();
		chart_data1.addColumn('datetime', 'Time of Day');
		chart_data1.addColumn('number', 'smoothed AWD');
		chart_data1.addColumn('number', 'AWD');
		chart_data1.addColumn('number', 'avg AWD');

		var chart_data2 = new google.visualization.DataTable();
		chart_data2.addColumn('datetime', 'Time of Day');
		chart_data2.addColumn('number', 'AWS');

		var smoothed = 0.0;
		for (var i=0; i < js_wind.length; i++){
			end = i;
			start = i-RANGE;
			if (start < 0) {
				start = 0;
			}
		    avgAWD = avgWindDirection(start, end, 7);
			if (i==0) {
				smoothed = js_wind[i][7];

			} else {
				//smoothed = alpha*js_wind[i][7]+(1-alpha)*smoothed;
				smoothed = smoothWindDirection(smoothed, js_wind[i][7], alpha);
			}
			chart_data1.addRow([new Date(Date.parse(js_wind[i][0])), smoothed, js_wind[i][7], avgAWD]);
			chart_data2.addRow([new Date(Date.parse(js_wind[i][0])), js_wind[i][8]]);
			//console.log("all Q: "+js_wind[i][0]+" | "+new Date(Date.parse(js_wind[i][0])));
		}
        var options = {
          series: {
            0: { color: 'FF0000' },
            1: { color: '0000FF', lineWidth: 1, lineDashStyle: [2, 1] },
            2: { color: '202020', lineWidth: 2, lineDashStyle: [4, 2] }
          }
        };
        var title = 'Apparent Wind Direction and Speed (AWD avg = '+avgAWDall+')';
        var yAxis = 'Apparent Wind Direction difference from Average AWD';
		plot_chart(chart_data1, options, title, yAxis, true);
        yAxis = 'Apparent Wind Speed (kts)';
		plot_chart(chart_data2, options, title, yAxis, legend, true);
	}

	function build_race_dropdown(race, race_id) {
		var html = "<select id='race_selection'>"; 
		//alert("race_id in build_race_dropdown ="+race_id);
		for (var i=0; i<race.length-1; i++) {
			if (race_id == race[i][race[i].length-1]) {
				html += "<option value='"+race[i][race[i].length-1]+"' selected>"+race[i][0]+"</option>";
			} else {
				html += "<option value='"+race[i][race[i].length-1]+"'>"+race[i][0]+"</option>";
			}
		}
		html += "</select>";
		document.getElementById("raceSelectionDropdown").innerHTML = html;
	}

	function update_wind_and_plot(race_id, charttype) {
    	//alert("url: bin/fetch_race_data.php?race="+race_id);
	    jQuery.ajax({
			dataType: "json",
	    	url: "bin/fetch_race_data.php?race="+race_id,
	    	success: function(data, textStatus, jqXHR){
	    		//console.log(JSON.stringify(data));
	    		//console.log("status: "+data['status']);
	    		if (data['status'] == "Success") {
	    			js_wind = data['data']['wind'];
	    			avgAWDall = data['data']['avgAWDall'];
	    			avgTWDall = data['data']['avgTWDall'];
	    			select_charttype(charttype);
	    		} else {
					alert("Ajax Error (2000) retrieving detailed Wind data to plot. "+textStatus+" | "+dataJSON);	    			
	    		}
			},
			error: function(jqXHR, textStatus, errorThrown){
				alert("Ajax Error (2001) retrieving detailed Wind data to plot. "+textStatus+" | "+errorThrown);
			}
		}); // jQuery Ajax function
	}

	function select_charttype(charttype) {
  		if (charttype == "perQuadrant") {
			google.setOnLoadCallback(all_lines(true));
  		} else if(charttype == "allQuadrants")  {
			google.setOnLoadCallback(all_quadrants(false));
  		} else if(charttype == "windSpeed")  {
			google.setOnLoadCallback(aws_tws(true));
   		} else if(charttype == "windDirection")  {
			google.setOnLoadCallback(awd_twd(true));
  		} else if(charttype == "trueDirectionSpeed")  {
			google.setOnLoadCallback(twd_tws(false));
  		} else if(charttype == "appDirectionSpeed")  {
			google.setOnLoadCallback(awd_aws(false));
 		}
	}

	function smoothWindDirection(smoothed, direction, alpha) {
		var smoothedX = Math.cos(toRad(smoothed));
		var smoothedY = Math.sin(toRad(smoothed));
		var dirX = Math.cos(toRad(direction));
		var dirY = Math.sin(toRad(direction));

		dirX = alpha*dirX + (1.0-alpha)*smoothedX; 
		dirY = alpha*dirY + (1.0-alpha)*smoothedY;

		if (Math.abs(dirX)<0.0000001 && Math.abs(dirY)<0.0000001) {
			return 0.0;
		} else {
			var dir = (360.0+toDegrees(Math.atan2(dirY, dirX))) % 360.0;
			return headingDelta(0.0, dir);
		}

	}

	function avgWindDirection(start, end, index) {
		var avgX = 0.0;
		var avgY = 0.0;
		for (var i=start; i < end; i++) {
			avgX = avgX + Math.cos(toRad(js_wind[i][index]));
			avgY = avgY + Math.sin(toRad(js_wind[i][index]));
		}
		avgX = avgX / js_wind.length;
		avgY = avgY / js_wind.length;

		if (Math.abs(avgX)<0.0000001 && Math.abs(avgY)<0.0000001) {
			return 0.0;
		} else {
			var dir = (360.0+toDegrees(Math.atan2(avgY, avgX))) % 360.0;
			return headingDelta(0.0, dir);
		}
	}

	function StdDevWind(start, end, avg, index) {
		var error = 0.0
		for (var i=start; i < end; i++) {
			error += (js_wind[i][index]-avg)*(js_wind[i][index]-avg);
		}
		return Math.sqrt(error/(end-start));
	}

	function headingDelta(From, To) {
		if (From > 360.0 || From < 0.0) From = From % 360.0;
		if (To > 360.0 || To < 0.0) To = To % 360.0;

		diff = To - From;
		absDiff = Math.abs(diff);

		if (absDiff <= 180.0) {
			if (absDiff == 180.00) diff = absDiff;
			return diff;
		} else if (To > From) {
			return (absDiff - 360.0);
		} else {
			return (360.0 - absDiff);
		}
	}

	function toRad(x) {
		return x*Math.PI / 180.0;
	}

	function toDegrees(x) {
		return x*180.0/Math.PI;
	}
	//==============================================================================================================
	// end of the function defitions
	//==============================================================================================================


	// default code run on launch of this script
	<?php
		// write the initial php variables to javascript
		echo($race_js);
		if (isset($GLOBALS['$race_id'])) {
			echo("var race_id = ".$GLOBALS['$race_id'].";\r");
		} else {
			echo("var race_id = race[0][7];\r");

		}
		// fetch the detailed wind data for the specified race_id
		echo(fetch_wind($GLOBALS['$race_id'], "chart"));
	?>

	build_race_dropdown(race, race_id);
	google.load("visualization", "1", {packages:["corechart"]});

	jQuery(document).ready(function(){
		var dropdown = jQuery("#charttype_selection").val();
		select_charttype(dropdown);
	  	jQuery("#charttype_selection").change(function(e) {
			e.preventDefault(); //STOP default action
	  		dropdown = jQuery("#charttype_selection").val();
	  		//alert("chart: "+dropdown);
	  		select_charttype(dropdown);
	  	});

	  	jQuery("#race_selection").change(function(e) {
			e.preventDefault(); //STOP default action
	  		dropdown = jQuery("#race_selection").val();
	  		//alert("race: "+dropdown);
	  		update_wind_and_plot(dropdown, jQuery("#charttype_selection").val());
	  	});
 	}); // end of jQuery(document).ready(function()

</script>

<?php
require "offline_footer.php"; 
?>
