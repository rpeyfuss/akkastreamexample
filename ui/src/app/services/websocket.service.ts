
import { Injectable } from '@angular/core';
// import { Message } from '../model/message';
//import { Event } from '../model/event';

import * as Rx from 'rxjs';
import {Subject} from "rxjs";

const SERVER_URL = 'http://localhost:9020';

@Injectable()
export class WebsocketService {

 private subject: Rx.Subject<MessageEvent>;
 private ws: any;

 constructor(){}

  public connect(url: string): Rx.Subject<MessageEvent> {
    if(!this.subject){
      this.subject = this.create(url);
      console.log("successfully connected: " + url);
    }
    return this.subject;
  }

  private create(url: string): Rx.Subject<any> {
    this.ws = new WebSocket(url);
    const observable = Rx.Observable.create(
      (obs: Rx.Observer<MessageEvent>) => {
        this.ws.onmessage = obs.next.bind(obs);
        this.ws.onerror = obs.error.bind(obs);
        this.ws.onclose = obs.complete.bind(obs);
        return this.ws.close.bind(this.ws);
      }).share();

    const observer = {
      next: (data: Object)=> {
        if (this.ws.readyState === WebSocket.OPEN){
          this.ws.send(JSON.stringify(data));
        }
      }
    };
    return Rx.Subject.create(observer, observable);
  }

  public close() {
    if(this.ws){
      this.ws.close();
      this.subject = null;
    }
  }


}
