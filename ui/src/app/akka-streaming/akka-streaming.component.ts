import { Component, OnInit } from '@angular/core';
import {Average, FileService} from "../services/file.service";

@Component({
  selector: 'app-akka-streaming',
  templateUrl: './akka-streaming.component.html',
  styleUrls: ['./akka-streaming.component.css']
})
export class AkkaStreamingComponent implements OnInit {
  data: Average[] = [];


  constructor(private fileService: FileService) {
    fileService.fileData.subscribe(msg => {
      console.log("response from websocket")
      console.log(msg);
      this.data =  this.data.concat(msg)
      console.log(this.data)
    })

  }

  ngOnInit() {
  }
}
