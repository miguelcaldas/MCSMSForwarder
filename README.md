# MC SMS Forwarder

A simple Android application to automatically forward SMS messages based on sender and content rules.

## Features

- **Rule-Based Forwarding**: Forwards SMS and RCS/MMS messages to a specified phone number.
- **Sender Filtering**: Filters messages based on a customizable list of sender phone numbers or contact names.
- **Content Filtering**: Uses a customizable list of regular expressions to filter messages by their content.
- **Persistent Configuration**: All settings and filter lists are saved and survive app restarts.
- **Background Operation**: Listens for and processes incoming SMS messages even when the app is not in the foreground.
- **Activity Log**: Displays a color-coded log of all received, forwarded, and blocked messages.
- **Filter Testing**: Includes a tool to test your filter rules with a sample message.
- **Permissions Handling**: Guides the user to grant the necessary permissions for operation.

## Setup

1.  Create a new project in Android Studio.
2.  Copy the files from this repository into the project structure.
3.  Build and run the app on an Android device or emulator.
4.  Grant the required permissions when prompted (Receive SMS, Send SMS, Read Contacts).
5.  Configure the target phone number, sender filters, and regex filters in the app.
