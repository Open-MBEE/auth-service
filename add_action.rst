Adding Issues to Flexo Project
==========================================

This document explains how to use the Github action https://github.com/marketplace/actions/add-to-github-projects to add
issues to the Flexo project.

This tutorial assumes that you have already installed Docker and NPX onto your machine.

Follow the directions within the link https://github.com/marketplace/actions/add-to-github-projects to create your own workflow. Once
you have created the workflow, go ahead and add it into the following directory: .github/workflows. If this directory does not exist,
go ahead and create it.

You can test if the workflow works by installing nektos https://github.com/nektos/act and running the following command:
::
    act -s GITHUB_TOKEN=<your github token>

