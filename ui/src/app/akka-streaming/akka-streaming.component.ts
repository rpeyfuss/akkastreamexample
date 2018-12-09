import { Component, OnInit } from '@angular/core';
import {FileService} from "../services/file.service";

@Component({
  selector: 'app-akka-streaming',
  templateUrl: './akka-streaming.component.html',
  styleUrls: ['./akka-streaming.component.css']
})
export class AkkaStreamingComponent implements OnInit {

  constructor(private fileService: FileService) {
    fileService.fileData.subscribe(msg => {
      console.log("response from websocket" + msg)
    })
  }

  ngOnInit() {
  }

}
