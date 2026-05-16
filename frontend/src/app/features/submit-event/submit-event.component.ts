import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-submit-event',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './submit-event.component.html',
  styleUrl: './submit-event.component.css'
})
export class SubmitEventComponent {

  constructor(private router: Router) {}

  readonly isValidJson = signal(true);

  payload = `{
  "eventId": "EVT-2025-05-28-001247",
  "eventType": "PRIVILEGED_ACCESS",
  "timestamp": "2025-05-28T14:31:15Z",

  "user": {
    "userId": "john.doe@bank.com",
    "role": "System Administrator",
    "department": "IT Operations"
  },

  "action": "LOGIN",

  "resource": {
    "system": "Core Banking System",
    "server": "cb-prod-01",
    "application": "CBS-Enterprise"
  },

  "source": {
    "ipAddress": "10.45.23.187",
    "location": "New York, USA",
    "device": "Windows 11 / Chrome"
  },

  "outcome": "SUCCESS",

  "sessionId": "sess_8f3a7b2d9c1e",

  "additionalData": {
    "loginMethod": "MFA",
    "accessLevel": "Elevated"
  }
}`;

  validateJson(): void {

    try {
      JSON.parse(this.payload);
      this.isValidJson.set(true);
    } catch {
      this.isValidJson.set(false);
    }

  }

  analyzeEvent(): void {

    this.validateJson();

    if (!this.isValidJson()) {
      return;
    }

    setTimeout(() => {
      this.router.navigate(['/analysis/EVT-2025-05-28-001247']);
    }, 1000);

  }

  clearPayload(): void {
    this.payload = '';
  }

}
