import { Injectable } from '@angular/core';
import {Subject} from "rxjs";
import {WebsocketService} from "./websocket.service";
import {map} from "rxjs/operators";


const FILE_URL = 'ws://echo.websocket.org/';

export class File {
  constructor(
    public author: string,
    public message: string
  ){}

}

@Injectable({
  providedIn: 'root'
})
export class FileService {
  public fileData: Subject<File>;

  constructor(wsService: WebsocketService) {
    this.fileData = <Subject<File>>wsService
      .connect(FILE_URL)
      .pipe(
        map((response: MessageEvent): File => {
          let data = JSON.parse(response.data);
          return{
            author: data.author,
            message: data.message
          }
        })
      );
  }
}
