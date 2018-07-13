<!DOCTYPE html>
<!--
//======================================================================================== 
//    HTML5 Webpage for the Capri 25 Offline Race Team
//	
//    offline_footer.php - common Footer for all Offline website pages
//
//	  losely build upon the free, responsive Corlate template
//	
//    (c) KaiserWare, Volker Petersen, January 2016
//
//======================================================================================== 
-->
<?php
if ($mysqli != "") {
	$mysqli->close();
}
?>
<footer id="footer" class="midnight-blue">
	<div class="container-fluid">
		<div class="hidden-xs col-sm-8 text-left">
			&copy; 2015-<?php echo(date("Y")); ?> Offline Capri-25 Race Team | Design & Implementation: KaiserWare&#0153;<br />
			Wayzata Yacht Club
		</div>
		<div class="hidden-xs col-sm-4 text-right">
			<ul class="pull-right">
				<li><a href="index.php">Home</a></li>
			</ul>
			<span><br /><?php echo(date($date_long)); ?></span>
		</div>
		<div class="col-xs-12 visible-xs-inline text-left">
			&copy; 2015-<?php echo(date("Y")); ?> Offline Capri-25 Race Team<br />
			Design & Implementation: KaiserWare&#0153;<br />
			Wayzata Yacht Club<br />
			<span><br /><?php echo(date($date_long)); ?></span>
		</div>
	</div>
</footer><!--/#footer-->
</body>
</html>