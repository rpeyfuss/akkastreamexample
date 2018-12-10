import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import { AkkaStreamingComponent } from './akka-streaming/akka-streaming.component';
import {FileService} from "./services/file.service";
import {WebsocketService} from "./services/websocket.service";

@NgModule({
  declarations: [
    AppComponent,
    AkkaStreamingComponent
  ],
  imports: [
    BrowserModule
  ],
  providers: [FileService, WebsocketService],
  bootstrap: [AppComponent]
})
export class AppModule { }
