---
layout: post
title: "Introducing Historia: A Tool for Tracking Test Coverage in Git Repositories"
date: 2025-03-20
---

# Introducing Historia

I am excited to announce the first official blog post for the Historia project! Historia is a specialized tool designed to help developers and teams analyze Git repositories to track the history of tests and requirements. It provides valuable insights into code quality and test coverage by identifying files that have been modified without corresponding test updates.

## What Does Historia Do?

At its core, Historia analyzes Git repositories to:

- Track file modifications over time
- Identify commits and pull requests that modify specific files
- Detect when code changes are made without corresponding test updates
- Generate reports showing statistics about untested updates for each file

This information is crucial for:

- Identifying areas of code with insufficient test coverage
- Tracking technical debt related to untested code
- Prioritizing testing efforts for frequently modified files
- Understanding the history of code and test changes

## Alpha Releases

As of today, I have released two alpha versions of Historia:

### Alpha 1: Initial Proof of Concept

Our first alpha release provided the very initial Proof of Concept for the project. It included the basic functionality to:

- Clone and analyze Git repositories using JGit
- Track file modifications over time
- Detect when code changes are made without corresponding test updates
- Generate basic CSV reports with statistics on file updates and test coverage

### Alpha 2: Pull Request Support

The second alpha release added a significant enhancement: support for Pull Requests when looking at commit history. This feature allows Historia to:

- Identify and track pull requests that include changes to specific files
- Group commits by pull request
- Analyze whether any commit in a pull request affects test files
- Provide more accurate tracking of untested updates by considering the PR context

## How Historia Works

Historia uses JGit to interact with Git repositories and analyze their history. The core functionality is currently implemented in the `UntestedCommitDetectionStrategy` class, which:

1. Processes each file in the repository
2. Finds all pull requests that include changes to the file
3. Checks if any commit in the pull request affects test files
4. Tracks statistics about updates and untested updates for each file

The tool generates a CSV report with the following information:

- **Module**: The module or prefix of the file
- **File**: The file path
- **# updates**: Total number of updates to the file
- **# untested updates**: Number of updates without corresponding test changes
- **# untested updates %**: Percentage of updates that were untested
- **# updates since last tested**: Number of consecutive updates without test changes

## Getting Started

Historia can be run using the following command:

```bash
java -cp core/target/historia-core-1.0.0-SNAPSHOT.jar org.jboss.historia.core.Runner org.jboss.historia.core.UntestedCommitDetectionStrategy ./output.csv src/main/java https://github.com/your-repo.git target/jgit/local-clone
```

Where:
- `org.jboss.historia.core.UntestedCommitDetectionStrategy` is the analysis strategy to use
- `./output.csv` is the path to the output CSV file
- `src/main/java` is a filter to limit analysis to specific file paths
- `https://github.com/your-repo.git` is the URI of the Git repository to analyze
- `target/jgit/local-clone` is the local path where the repository will be cloned

## Future Plans

I am continuing to develop Historia and plan to add more features in future releases. Stay tuned for updates on the project's development and new features!

## Get Involved

Historia is an open-source project, and we welcome contributions from the community. Check out the [GitHub repository](https://github.com/asoldano/historia) to learn more about the project and how you can get involved.
