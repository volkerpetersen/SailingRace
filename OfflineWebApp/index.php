<!DOCTYPE html>
<!--
//======================================================================================== 
//    HTML5 Webpage for the Capri 25 Offline Race Team
//	
//    index.php - Homepage of the Offline Capri-25 Race Team website
//
//	  losely build upon the free, responsive Corlate template
//	
//    (c) KaiserWare, Volker Petersen, January 2016
//
//======================================================================================== 
-->
<?php 
global $title;

$title = 'Offline Capri-25 Race Team';
require "offline_header.php";

?>
<section id="feature" class="body-page-start">
	<div class="container">
		<div class="row">
			<h1 class="col-xs-12 text-center" id="table_name" class="text-info">Offline SailingRace&#0153; App Documentation</h1>
		</div>

        <div class="row body-text">
			<div class="col-sm-3 col-xs-12 text-center">
				<br />&nbsp;
				<img class="img-responsive center-block" src="images/Main.png" alt="Offline Main Page">
			</div>
			<div class="col-sm-9 col-xs-12">
				<h2>Start Screen</h2>
				<p>The start screen contains 3 buttons to launch the activities</p>
				<ol>
					<li>Set Preferences</li>
					<li>Start the race timer.  On this screen you can also set the Leeward and Windward marks.</li>
					<li>Race mode</li>
				</ol>
				<p>Please make sure that the <strong>SailTimerAPI&#0153</strong> App is running in the background and that it is connected to your Bluetooth <strong>SailTimer</strong> Wind Annemometer.  Please see the SailTimerAPI documentation at the bottom of this document.</p>
				<p>At this time, please do not check the ceckbox <strong>Sync to Web</strong> as this App option is still in beta testing.</p>
			</div>
		</div><!--/.row-->

		</div><div class="container-fluid full-width"><hr></div><div class="container">
        <div class="row body-text">
			<div class="col-sm-3 col-xs-12 text-center">
				<br />&nbsp;
				<img class="img-responsive center-block" src="images/Preferences.png" alt="Offline Preferences">
			</div>
			<div class="col-sm-9 col-xs-12">
				<h2>Preferences</h2>
				<p>Screen to change the various App defaults.  If you enable the Capri-25 Polars, the App will utilize the Polar parameters to compute the tack-angle, gybe-angle, and target speeds (based on the wind speed).  If you don't have a Wind Anemometer, you can manually set a tack- and gybe-angle</p>
			</div>
		</div><!--/.row-->

		</div><div class="container-fluid full-width"><hr></div><div class="container">
        <div class="row body-text">
			<div class="col-sm-3 col-xs-12 text-center">
				<br />&nbsp;
				<img class="img-responsive center-block" src="images/Timer.png" alt="Offline Race Start Timer">
			</div>
			<div class="col-sm-9 col-xs-12">
				<h2>Race Timer</h2>
				<p>The defaults on this screen are set using the Preference Settings.  The defaults are:</p>
				<ol>
					<li>minutes/seconds between starts,</li>
					<li>your class.  The start sequence of all classes (e.g. S2, J/22, J/24, etc.) is hard-coded in this App.</li>
					<li>Start sequence starts with a Warning Period</li> 
				</ol>
				<p>During the race prep phase, use this sceen to set the Leeward Mark (LWD) and Windward Mark (WWD).  Press the <strong>LWD SET</strong> button while you are checking out the starting line during the pre-race prep.  Set the WWD mark by pressing the <strong>WWD SET</strong> button.  That will open a map showing a marker at the current boat loactaion.  When you press this mark, you can drag it towar the WWD Mark location.  Move the mark approximately 1nm upwind (distance and direction between boat and current WWD mark location are shown on the top of the screen).</p>
				<p>During the race you can reset the WWD mark as needed by pressing a <strong>RESET WWD</strong> button on top right of the <strong>Race Map</strong> screen.</p>
				<p>Hit the <strong>Start</strong> button to start the timer.  The App will automatically switch over to the Race screen when the start countdown has reached zero.</p>
				<p>When the countdown has started the <strong>Start</strong> button changes to a <strong>Stop</strong> button.  When you hit <strong>Stop</strong>, the button changes to <strong>RESET</strong> to allow you to reset the countdown timer.  The reset will be to the full duration of one class sequence time when the reset occurs before half of one class sequence time has elapsed.  If more than half of one class sequence time has elapsed, the reset will start the countdown of the next class sequence.</p> 
				<p>When swiping the screen to the left, the display changes from this <strong>Race Timer</strong> to <strong>Startline Map</strong> display showing the current startline marks (Committee Boat and Pin), the intersection of current course with the startline, and the favorite end of the start line.</p>
			</div>
		</div><!--/.row-->

		</div><div class="container-fluid full-width"><hr></div><div class="container">
        <div class="row body-text">
			<div class="col-sm-3 col-xs-12 text-center">
				<br />&nbsp;
				<img class="img-responsive center-block" src="images/Start_TabletView.png" alt="Offline Main Page">
			</div>
			<div class="col-sm-9 col-xs-12">
				<h2>Race Timer & Startline Map on Tablet</h2>
				<p>On a Tablet device the two screens (<strong>Race Timer</strong> and <strong>Startline Map</strong> are displayed side by side and there is no need to switch between these two display screens.</p>
			</div>
		</div><!--/.row-->

		</div><div class="container-fluid full-width"><hr></div><div class="container">
        <div class="row body-text">
			<div class="col-sm-3 col-xs-12 text-center">
				<br />&nbsp;
				<img class="img-responsive center-block" src="images/RaceInstruments.png" alt="Offline Race Instruments">
			</div>
			<div class="col-sm-9 col-xs-12">
				<h2>Race Instruments</h2>
				<p>Top of the screen indicates if WWD and LWD marks have been set and which of the two marks is currently "active" (which mark we are sailing toward).</p>
				<ul>
					<li type="square">The SOG goal is computed from the Capri-25 polars based on the current Wind Angle and Wind Speed.</li>
					<li type="square">The VMG is the speed vector directly upwind (downwind).  The goal VMG is the upwind vector of the goal SOG.  Between the goal SOG and goal VMG you have all the necessary information to determine if you are sailing the optimum course / speed toward the current mark.</li>
					<li type="square">The Wind Gauge displays the wind speed (Yellow number underneath the boat.  If this number is red, the Windex Bluetooth connection failed).  The white arrow shows the wind angle. The yellow angle shows the tack- or gybe-angle.  All figures can be switched between True and Apparent Wind values using the radio buttons (<strong>TRUE / APP</strong>).  The checkbox <strong>autotack</strong> allows you to alternate between the App determining your current course (toward WWD or LWD mark) and tack.  If the mark is not set, the user has to set the course tack by pressing on the boat in the Wind Gauge.  A short press tacks / gybes the boat.  A long press switches from racing toward WWD to LWD and visa versa.</li>
					<li type="square">When swiping the screen to the left or right, the display changes from this <strong>Race Instruments</strong> to <strong>Race Map</strong> display showing the current marks and laylines.</li>
				</ul>	
			</div>
		</div><!--/.row-->

		</div><div class="container-fluid full-width"><hr></div><div class="container">
        <div class="row body-text">
			<div class="col-sm-3 col-xs-12 text-center">
				<br />&nbsp;
				<img class="img-responsive center-block" src="images/RaceMap.png" alt="Offline Race Map">
			</div>
			<div class="col-sm-9 col-xs-12">
				<h2>Race Map</h2>
				<p>The screen shows the current boat COG, the laylines toward the active mark and the TWD / TWS.</p>
				<p>Use the <strong>RESET WWD</strong> button to reset the WWD mark at the current boat location (thus use this button to correct the WDD mark when you are at the WDD mark).</p>
				<p>Use the Android Back button to return to the <strong>Race Instrument</strong> screen.</p>
			</div>
		</div><!--/.row-->

		</div><div class="container-fluid full-width"><hr></div><div class="container">
        <div class="row body-text">
			<div class="col-sm-3 col-xs-12 text-center">
				<br />&nbsp;
				<img class="img-responsive center-block" src="images/TabletView.png" alt="Offline Main Page">
			</div>
			<div class="col-sm-9 col-xs-12">
				<h2>Race Instruments & Map on Tablet</h2>
				<p>On a Tablet device the two screens (<strong>Race Instruments</strong> and <strong>Race Map</strong>are displayed side by side and there is no need to switch between these two display screens.</p>
			</div>
		</div><!--/.row-->

		</div><div class="container-fluid full-width"><hr></div><div class="container">
        <div class="row body-text">
			<div class="col-sm-3 col-xs-12 text-center">
				<br />&nbsp;
				<img class="img-responsive center-block" src="images/MainHelp.png" alt="Offline Main Page with About overlay">
			</div>
			<div class="col-sm-9 col-xs-12">
				<h2>About & Help Screen</h2>
				<p>You can get to this screen by picking this option from the <strong>Start</strong> screen <strong>Settings</strong> submenu in the Top of the screen.</p>
			</div>
		</div><!--/.row-->

		</div><div class="container-fluid full-width"><hr></div><div class="container body-text">
        <div class="row">
			<div class="col-xs-12 text-left">
				<h2>SailTimerAPI&#0153; App</h2>
				<p>You need to launch this App and establish a <i>Bluetooth</i> connection between the smartphone/tablet and the SailTimer Wind Annemometer.  Please follow these steps:</p>
				<br />&nbsp;
	        </div>
	    </div>
        <div class="row">
			<div class="col-sm-3 col-xs-12 text-center">
				<img class="img-responsive center-block" src="images/SailTimer_01.png" alt="SailTimerAPI Main Screen">
				<br />&nbsp;
			</div>
			<div class="col-sm-9 col-xs-12">
				<p>1.) Select the SailTimer Wind Instrument from the list of <i>Bluetooth</i> devices by pressing on the SailTimer Wind Instrument text. Should the SailTimer Wind Instrument not show up on this list, please exit this App, shutdown <i>Bluetooth</i> on your smartphone (swipe down from the top of the screen and press the <i>Bluetooth</i> button), wait a couple minutes and restart <i>Bluetooth</i>, and then restart the SailTimerAPI App.</p> 					
			</div>
		</div><!--/.row-->
        <div class="row">
			<div class="col-sm-3 col-xs-12 text-center">
				<img class="img-responsive center-block" src="images/SailTimer_02.png" alt="SailTimerAPI Connect Screen">
				<br />&nbsp;
			</div>
			<div class="col-sm-9 col-xs-12">
				<p>2.) Wait for the app to establish a <i>Bluetooth</i> connection to the windex.  When a connection has been establish you can read this text above the four buttons: <i>"State: Please enable buttons below"</i></p> 				
			</div>
		</div><!--/.row-->
        <div class="row">
			<div class="col-sm-3 col-xs-12 text-center">
				<img class="img-responsive center-block" src="images/SailTimer_03.png" alt="SailTimerAPI Connect Screen 2">
				<br />&nbsp;
			</div>
			<div class="col-sm-9 col-xs-12">
				<p>3.) When the connection has been established, please press the two buttons (<i>"Enable Wind Direction"</i> and <i>"Enable Wind Speed"</i>.  Do not enable the other two buttons.<br />When this is done, please press the <strong>Home</strong> button on the device to leave this App running in the background.</p> 				
			</div>
		</div><!--/.row-->

		&nbsp;<BR />
	</div><!--/.container-->
</section><!--/#feature-->

<?php
require "offline_footer.php"; 
?>