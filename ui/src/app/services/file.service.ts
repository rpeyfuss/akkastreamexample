import { Injectable } from '@angular/core';
import {Subject} from "rxjs";
import {WebsocketService} from "./websocket.service";
import {map} from "rxjs/operators";


//const FILE_URL = 'ws://echo.websocket.org/';
const FILE_URL = 'ws://localhost:9020/data';

export interface Average {
 average: number
}



@Injectable({
  providedIn: 'root'
})
export class FileService {
  public fileData: Subject<String>;

  constructor(wsService: WebsocketService) {
    this.fileData = <Subject<String>>wsService
      .connect(FILE_URL)
      .pipe(
        map((response: MessageEvent): String => {
          let data:String = response.data;
          console.log(data);
          return data;
        })
      );
  }
}
