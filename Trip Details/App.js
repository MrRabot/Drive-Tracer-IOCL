
//Database initialize

import{initializeApp} from  "https://www.gstatic.com/firebasejs/9.15.0/firebase-app.js"
import{getDatabase, ref, onValue} from "https://www.gstatic.com/firebasejs/9.15.0/firebase-database.js"

var driverID = "sdk_gphone_x86_64/"
var tripID = "31:07:2023|00:45:27|OUT"

const appSettings = {
    databaseURL: "firebasedatabase.app/"//Enter your database reference URL
}

const app =  initializeApp(appSettings)
const database = getDatabase(app)
const databaseReference = ref(database, driverID+tripID)







//Driver details
document.getElementById("driver-details").innerHTML = "Driver 1";

let arr4 = tripID.split("|") 

document.getElementById("trip-details").innerHTML= arr4.join(" ")



//map optitons
//javascript.js
//set map options
var Location = { lat: 26.1828, lng: 91.8038 };
var mapOptions = {
    center: Location,
    zoom: 15,
    mapTypeId: google.maps.MapTypeId.ROADMAP

};



//create map
var map = new google.maps.Map(document.getElementById('googleMap'), mapOptions);

//initialize directions service

var directionsService = new google.maps.DirectionsService();

//initialize directions renderer(used to display the return form direction service)

var directionsRenderer = new google.maps.DirectionsRenderer();

directionsRenderer.setMap(map);

const cityCircle = new google.maps.Circle({
    strokeColor: "#FF0000",
    strokeOpacity: 0.8,
    strokeWeight: 2,
    fillColor: "#FF0000",
    fillOpacity: 0.35,
    map,
    center: Location,
    radius: 2000,
  });


//Read Database

onValue (databaseReference, function(snapshot){

    var locationList = Object.values(snapshot.val())

    let locNum = locationList.length;

    let arr1 = locationList[0].split('$')

    const startLocation = arr1.join(", ");

    let arr2 = locationList[locNum-1].split('$');

    const endLocation = arr2.join(", ");

    var waypts = [];


    for(let i = 1; i<locNum-1; i++){

        let arr3 = locationList[i].split('$');

        let latitude = parseFloat(arr3[0]);

        let longitude = parseFloat(arr3[1]);

        waypts.push({
            location: { lat: latitude, lng: longitude},
            stopover: false,
        });
    
    }


    calcRoute(startLocation, endLocation, waypts)


})


//function

function calcRoute(startLocation, endLocation, waypts){

    var request = {
        origin: startLocation,
        destination: endLocation,
        waypoints: waypts,
        travelMode: google.maps.TravelMode.DRIVING, //WALKING, BYCYCLING, TRANSIT
        unitSystem: google.maps.UnitSystem.METRIC
    }

    directionsService.route(request, (result, status)=>{
        if(status==google.maps.DirectionsStatus.OK){
            //get distance and time
            const output = document.querySelector('#output');
            output.innerHTML = "<div class='alert-info'>From: " +startLocation+ ".<br />To: " + endLocation+ ".<br /> Driving distance <i class='fas fa-road'></i> : " + result.routes[0].legs[0].distance.text + ".<br />Duration <i class='fas fa-hourglass-start'></i> : " + result.routes[0].legs[0].duration.text + ".</div>";

            //render display
            directionsRenderer.setDirections(result);
        }else{
            //delete route from map
            directionsRenderer.setDirections({ routes: []});

            map.setCenter(Location);

            //show error msg
            output.innerHTML = "<div class='alert-danger'><i class='fa-solid fa-circle-exclamation'></i> Could not retrive driving distance. </div>"
        }
    });
}

