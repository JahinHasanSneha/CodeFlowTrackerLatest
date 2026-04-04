# CodeFlowTrackerLatest
# CodeFlow Tracker

CodeFlow Tracker is a multi-module Java application designed for coding students who want one place to manage study life, coding practice, deadlines, and collaboration.

It combines a JavaFX desktop client with a lightweight Java backend server and provides a focused workspace for planning tasks, tracking LeetCode progress, organizing courses, and staying connected with friends.

## What The Project Presents

CodeFlow Tracker presents itself as a smart study companion for learners in tech. Instead of splitting work across separate apps for to-do lists, coding practice, calendars, study sessions, notes, and communication, the platform brings everything into a single experience.

The product is built around productivity, consistency, and student collaboration.

## Core Features

### Authentication
- User sign-up and login
- Password hashing for secure credential storage
- Token-based authentication for protected API access

### Dashboard
- Quick overview of daily activity
- Displays today's tasks
- Shows active task count and completed-today count
- Highlights upcoming projects

### Task Management
- Create tasks with title, description, priority, due date, and category
- Update and complete tasks
- Delete tasks
- Filter tasks by all, today, active, or completed

### Project Calendar
- Add and manage projects with deadlines
- Monthly calendar view
- Upcoming projects tracking
- Visual organization of due dates

### Progress Tracking
- Daily coding progress tracking
- Total solved problems statistics
- Weekly activity summary
- GitHub-style heatmap for contribution-style progress visualization

### LeetCode Tracking
- Add LeetCode problems manually
- Track problem difficulty
- Mark problems as solved or unsolved
- Filter by difficulty and status
- Monitor total solved progress

### Pomodoro Focus Timer
- 25-minute focus timer
- Pause and reset controls
- Study-session themed UI
- Optional built-in study music

### Friends System
- Send friend requests
- Accept or decline incoming requests
- View current friends
- Remove or block users

### Messaging
- One-to-one messaging between friends
- Conversation list
- Live polling for new messages
- Persistent chat history

### Course Hub
- Add and manage courses
- Dedicated course pages
- Organize course-related materials in one place

### Course Workspace
Each course can store:
- Notes
- Resources
- Slides
- Course-specific tasks

## Tech Stack

- Java 17
- JavaFX for the desktop frontend
- Maven multi-module project structure
- Java built-in HTTP server for backend APIs
- Jackson for JSON serialization
- jBCrypt for password hashing
- File-based JSON storage for persistence

## Project Structure

```text
multimodule/
├── client/   # JavaFX desktop application
├── server/   # HTTP backend server
└── pom.xml   # parent Maven module
```

## How To Run

### Prerequisites
- Java 17 installed
- Maven installed

### Run the server

From the `multimodule` directory:

```bash
mvn compile exec:java -pl server
```

The server starts by default on port `8080` unless a `PORT` environment variable is provided.

### Run the client

From the `multimodule` directory:

```bash
mvn javafx:run -pl client
```

## Data Storage

The backend uses JSON files for persistence instead of a traditional database.

- If `DATA_DIR` is set, data is stored there
- Otherwise it tries `/data/codeflow`
- If that is not available, it falls back to `~/.codeflow/data`

This makes the project simple to run locally and easy to deploy in lightweight environments.

## Status Summary

CodeFlow Tracker is currently a functional study and productivity platform for coding students, with implemented support for planning, coding progress tracking, focus sessions, course organization, and social interaction.

## Commercial Summary

CodeFlow Tracker is an all-in-one productivity and study companion for coding students. It helps users stay organized, track practice, manage deadlines, structure course content, and collaborate with peers in a single desktop platform.
